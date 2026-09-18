package me.wyne.infpoints.storage.sql;

import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.field.DatabaseFieldConfig;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.j256.ormlite.table.TableUtils;
import me.wyne.infpoints.api.transaction.TransactionResult.Status;
import me.wyne.infpoints.api.transaction.TransactionType;
import me.wyne.infpoints.point.Amounts;
import me.wyne.infpoints.point.PointDefinition;
import me.wyne.infpoints.storage.AuditReport;
import me.wyne.infpoints.storage.LedgerEntry;
import me.wyne.infpoints.storage.Mutation;
import me.wyne.infpoints.storage.StorageException;
import me.wyne.infpoints.storage.StorageResult;
import me.wyne.infpoints.storage.StorageUnavailableException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTransactionRollbackException;
import java.sql.SQLTransientConnectionException;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class LedgerDatabase implements AutoCloseable {

    private static final int SCHEMA_VERSION = 1;
    private static final int MAX_ATTEMPTS = 8;
    private static final int IMPORT_BATCH_SIZE = 500;
    private static final int H2_LOCK_TIMEOUT = 50200;
    private static final String INTERNAL_SOURCE = "InfPoints";
    private static final String SCHEMA_VERSION_KEY = "schema.version";
    private static final String DECIMALS_KEY = "decimals.";
    private static final String LEGACY_TABLE_KEY = "migration.legacy-sql.";
    private static final String LEGACY_JSON_KEY = "migration.legacy-json.";

    private final Supplier<@Nullable ConnectionSource> sources;
    private final Supplier<LedgerSettings> settings;
    private final BooleanSupplier mainThread;
    private final Supplier<Logger> logger;
    private final LegacyImports legacyImports;
    private final CircuitBreaker breaker = new CircuitBreaker(3, Duration.ofSeconds(10));
    private final ExecutorService connectionExecutor;
    private final Set<Object> prepared = ConcurrentHashMap.newKeySet();
    private final Object preparationLock = new Object();
    private volatile @Nullable Context context;

    public LedgerDatabase(@NotNull Supplier<@Nullable ConnectionSource> sources, @NotNull Supplier<LedgerSettings> settings,
                          @NotNull BooleanSupplier mainThread, @NotNull Supplier<Logger> logger, @NotNull LegacyImports legacyImports) {
        this.sources = sources;
        this.settings = settings;
        this.mainThread = mainThread;
        this.logger = logger;
        this.legacyImports = legacyImports;
        this.connectionExecutor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "InfPoints Connection");
            thread.setDaemon(true);
            return thread;
        });
    }

    private record Context(ConnectionSource source, LedgerSettings settings, LedgerSql sql) {}

    private record PreparedSchema(ConnectionSource source, String prefix) {}

    private record PreparedPoint(ConnectionSource source, String prefix, String point, int decimals) {}

    private record LegacyBalance(String player, double balance) {}

    private static final class ImportCount {
        private long imported;
        private long skipped;
        private long invalid;
        private BigDecimal rounding = BigDecimal.ZERO;
    }


    public void prepare(@NotNull PointDefinition definition) throws StorageException {
        ensurePrepared(context(), definition);
    }

    public long balance(@NotNull PointDefinition definition, @NotNull UUID player) throws StorageException {
        Context context = context();
        ensurePrepared(context, definition);
        try (LedgerSession session = open(context)) {
            OptionalLong balance = selectBalance(session, context, definition.key(), player);
            session.commit();
            breaker.recordSuccess();
            return balance.orElse(definition.defaultUnits());
        } catch (SQLException e) {
            throw translate(e, "read a balance of point '" + definition.key() + "'");
        }
    }

    public @NotNull Map<UUID, Long> balances(@NotNull PointDefinition definition, int offset, int limit) throws StorageException {
        Context context = context();
        ensurePrepared(context, definition);
        try (LedgerSession session = open(context)) {
            Map<UUID, Long> balances = new LinkedHashMap<>();
            try (PreparedStatement statement = session.prepare(context.sql().selectAccounts)) {
                statement.setString(1, definition.key());
                statement.setInt(2, limit);
                statement.setInt(3, offset);
                try (ResultSet results = statement.executeQuery()) {
                    while (results.next())
                        balances.put(UUID.fromString(results.getString(1)), results.getLong(2));
                }
            }
            session.commit();
            breaker.recordSuccess();
            return balances;
        } catch (SQLException e) {
            throw translate(e, "read the balances of point '" + definition.key() + "'");
        }
    }

    public @NotNull StorageResult apply(@NotNull PointDefinition definition, @NotNull Mutation mutation) throws StorageException {
        Context context = context();
        ensurePrepared(context, definition);
        for (int attempt = 1; ; attempt++) {
            try (LedgerSession session = open(context)) {
                if (mutation.idempotencyKey() != null) {
                    List<LedgerEntry> original = selectByIdempotencyKey(session, context, definition.key(), mutation.idempotencyKey());
                    session.commit();
                    if (!original.isEmpty())
                        return duplicate(definition, mutation, original);
                }
                ensureAccount(session, context, definition, mutation.player());
                if (mutation.receiver() != null)
                    ensureAccount(session, context, definition, mutation.receiver());
                StorageResult result = applyInTransaction(session, context, definition, mutation);
                breaker.recordSuccess();
                return result;
            } catch (SQLException e) {
                if (isRetryable(e) && attempt < MAX_ATTEMPTS) {
                    backoff(attempt);
                    continue;
                }
                if (mutation.idempotencyKey() != null) {
                    StorageResult duplicate = duplicateAfterFailure(context, definition, mutation);
                    if (duplicate != null)
                        return duplicate;
                }
                throw translate(e, "apply " + mutation.operation() + " to point '" + definition.key() + "'");
            }
        }
    }

    public @NotNull List<LedgerEntry> history(@NotNull PointDefinition definition, @NotNull UUID player, int offset, int limit) throws StorageException {
        Context context = context();
        ensurePrepared(context, definition);
        try (LedgerSession session = open(context); PreparedStatement statement = session.prepare(context.sql().selectHistory)) {
            statement.setString(1, definition.key());
            statement.setString(2, player.toString());
            statement.setInt(3, Math.max(0, limit));
            statement.setInt(4, Math.max(0, offset));
            List<LedgerEntry> entries = readEntries(statement);
            session.commit();
            breaker.recordSuccess();
            return entries;
        } catch (SQLException e) {
            throw translate(e, "read the history of point '" + definition.key() + "'");
        }
    }

    public @NotNull Optional<LedgerEntry> transaction(@NotNull PointDefinition definition, long id) throws StorageException {
        Context context = context();
        ensurePrepared(context, definition);
        try (LedgerSession session = open(context); PreparedStatement statement = session.prepare(context.sql().selectEntry)) {
            statement.setString(1, definition.key());
            statement.setLong(2, id);
            List<LedgerEntry> entries = readEntries(statement);
            session.commit();
            breaker.recordSuccess();
            return entries.stream().findFirst();
        } catch (SQLException e) {
            throw translate(e, "read a transaction of point '" + definition.key() + "'");
        }
    }

    public @NotNull AuditReport audit(@NotNull PointDefinition definition, @Nullable UUID player) throws StorageException {
        Context context = context();
        ensurePrepared(context, definition);
        LedgerSql sql = context.sql();
        try (LedgerSession session = open(context)) {
            int checked;
            try (PreparedStatement statement = session.prepare(player == null ? sql.auditCount : sql.auditCountOfPlayer)) {
                statement.setString(1, definition.key());
                if (player != null)
                    statement.setString(2, player.toString());
                try (ResultSet results = statement.executeQuery()) {
                    checked = results.next() ? results.getInt(1) : 0;
                }
            }
            List<AuditReport.Mismatch> mismatches = new ArrayList<>();
            try (PreparedStatement statement = session.prepare(player == null ? sql.auditMismatches : sql.auditMismatchesOfPlayer)) {
                statement.setString(1, definition.key());
                if (player != null)
                    statement.setString(2, player.toString());
                try (ResultSet results = statement.executeQuery()) {
                    while (results.next())
                        mismatches.add(new AuditReport.Mismatch(UUID.fromString(results.getString(1)), results.getLong(2), results.getLong(3)));
                }
            }
            session.commit();
            breaker.recordSuccess();
            return new AuditReport(checked, mismatches);
        } catch (SQLException e) {
            throw translate(e, "audit point '" + definition.key() + "'");
        }
    }

    @Override
    public void close() {
        connectionExecutor.shutdown();
    }

    private StorageResult applyInTransaction(LedgerSession session, Context context, PointDefinition definition, Mutation mutation) throws SQLException {
        String point = definition.key();
        UUID player = mutation.player();
        long units = mutation.units();
        long now = System.currentTimeMillis();
        LedgerSql sql = context.sql();
        switch (mutation.operation()) {
            case ADD -> {
                if (update(session, sql.addBalance, units, now, point, player, Long.MAX_VALUE - units) == 0)
                    return abort(session, context, point, player, Status.FAILED);
                long balance = requireBalance(session, context, point, player);
                LedgerEntry entry = insertEntry(session, context, point, player, TransactionType.ADD, units, balance, null, mutation.idempotencyKey(), mutation, now);
                session.commit();
                return StorageResult.success(balance, List.of(entry));
            }
            case SUBTRACT -> {
                if (update(session, sql.subtractBalance, units, now, point, player, units) == 0)
                    return abort(session, context, point, player, Status.INSUFFICIENT_FUNDS);
                long balance = requireBalance(session, context, point, player);
                LedgerEntry entry = insertEntry(session, context, point, player, TransactionType.SUBTRACT, -units, balance, null, mutation.idempotencyKey(), mutation, now);
                session.commit();
                return StorageResult.success(balance, List.of(entry));
            }
            case SET -> {
                // Locking the row first means the balance read below can't change before it is replaced
                try (PreparedStatement statement = session.prepare(sql.lockAccount)) {
                    statement.setLong(1, now);
                    statement.setString(2, point);
                    statement.setString(3, player.toString());
                    if (statement.executeUpdate() == 0)
                        throw new SQLException("Account of " + player + " for point '" + point + "' doesn't exist");
                }
                long current = requireBalance(session, context, point, player);
                long delta;
                try {
                    delta = Math.subtractExact(units, current);
                } catch (ArithmeticException e) {
                    session.rollback();
                    return StorageResult.failure(Status.FAILED, current);
                }
                try (PreparedStatement statement = session.prepare(sql.setBalance)) {
                    statement.setLong(1, units);
                    statement.setLong(2, now);
                    statement.setString(3, point);
                    statement.setString(4, player.toString());
                    statement.executeUpdate();
                }
                LedgerEntry entry = insertEntry(session, context, point, player, TransactionType.SET, delta, units, null, mutation.idempotencyKey(), mutation, now);
                session.commit();
                return StorageResult.success(units, List.of(entry));
            }
            case TRANSFER -> {
                UUID receiver = mutation.receiver();
                if (receiver == null)
                    throw new SQLException("Transfer without a receiver");
                // Rows are always locked in the same order so opposite transfers can't deadlock each other
                boolean senderFirst = player.toString().compareTo(receiver.toString()) < 0;
                if (senderFirst && update(session, sql.subtractBalance, units, now, point, player, units) == 0)
                    return abort(session, context, point, player, Status.INSUFFICIENT_FUNDS);
                if (update(session, sql.addBalance, units, now, point, receiver, Long.MAX_VALUE - units) == 0)
                    return abort(session, context, point, player, Status.FAILED);
                if (!senderFirst && update(session, sql.subtractBalance, units, now, point, player, units) == 0)
                    return abort(session, context, point, player, Status.INSUFFICIENT_FUNDS);
                long senderBalance = requireBalance(session, context, point, player);
                long receiverBalance = requireBalance(session, context, point, receiver);
                UUID correlation = UUID.randomUUID();
                LedgerEntry out = insertEntry(session, context, point, player, TransactionType.TRANSFER_OUT, -units, senderBalance, correlation, mutation.idempotencyKey(), mutation, now);
                LedgerEntry in = insertEntry(session, context, point, receiver, TransactionType.TRANSFER_IN, units, receiverBalance, correlation, null, mutation, now);
                session.commit();
                return StorageResult.success(senderBalance, List.of(out, in));
            }
        }
        throw new SQLException("Unknown operation " + mutation.operation());
    }

    private StorageResult abort(LedgerSession session, Context context, String point, UUID player, Status status) throws SQLException {
        session.rollback();
        OptionalLong balance = selectBalance(session, context, point, player);
        session.commit();
        return StorageResult.failure(status, balance.orElse(StorageResult.UNKNOWN_BALANCE));
    }

    private void ensureAccount(LedgerSession session, Context context, PointDefinition definition, UUID player) throws SQLException {
        boolean exists = selectBalance(session, context, definition.key(), player).isPresent();
        session.commit();
        if (exists)
            return;
        long now = System.currentTimeMillis();
        try {
            insertAccount(session, context, definition.key(), player, definition.defaultUnits(), now);
            insertEntry(session, context, definition.key(), player, TransactionType.INITIAL, definition.defaultUnits(), definition.defaultUnits(),
                    null, null, INTERNAL_SOURCE, null, null, now);
            session.commit();
        } catch (SQLException e) {
            session.rollback();
            // Another server may have opened the same account at the same moment
            boolean created = selectBalance(session, context, definition.key(), player).isPresent();
            session.commit();
            if (!created)
                throw e;
        }
    }

    private StorageResult duplicate(PointDefinition definition, Mutation mutation, List<LedgerEntry> original) {
        LedgerEntry first = original.get(0);
        if (!matches(first, mutation))
            logger.get().warn("Idempotency key '{}' of point '{}' was reused for a different request, returning the original transaction #{}",
                    mutation.idempotencyKey(), definition.key(), first.id());
        long balance = original.stream()
                .filter(entry -> entry.player().equals(mutation.player()))
                .findFirst()
                .map(LedgerEntry::balance)
                .orElse(StorageResult.UNKNOWN_BALANCE);
        return new StorageResult(Status.DUPLICATE, balance, original);
    }

    private static boolean matches(LedgerEntry entry, Mutation mutation) {
        if (!entry.player().equals(mutation.player()))
            return false;
        return switch (mutation.operation()) {
            case ADD -> entry.type() == TransactionType.ADD && entry.units() == mutation.units();
            case SUBTRACT -> entry.type() == TransactionType.SUBTRACT && entry.units() == -mutation.units();
            case SET -> entry.type() == TransactionType.SET && entry.balance() == mutation.units();
            case TRANSFER -> entry.type() == TransactionType.TRANSFER_OUT && entry.units() == -mutation.units();
        };
    }

    private @Nullable StorageResult duplicateAfterFailure(Context context, PointDefinition definition, Mutation mutation) {
        try (LedgerSession session = open(context)) {
            List<LedgerEntry> original = selectByIdempotencyKey(session, context, definition.key(), mutation.idempotencyKey());
            session.commit();
            return original.isEmpty() ? null : duplicate(definition, mutation, original);
        } catch (SQLException | StorageException e) {
            return null;
        }
    }

    private void ensurePrepared(Context context, PointDefinition definition) throws StorageException {
        PreparedPoint preparedPoint = new PreparedPoint(context.source(), context.settings().tablePrefix(), definition.key(), definition.decimals());
        if (prepared.contains(preparedPoint))
            return;
        synchronized (preparationLock) {
            if (prepared.contains(preparedPoint))
                return;
            try {
                PreparedSchema preparedSchema = new PreparedSchema(context.source(), context.settings().tablePrefix());
                if (!prepared.contains(preparedSchema)) {
                    createSchema(context);
                    prepared.add(preparedSchema);
                }
                try (LedgerSession session = open(context)) {
                    checkDecimals(session, context, definition);
                    importLegacyTable(session, context, definition);
                    importLegacyJson(session, context, definition);
                }
                breaker.recordSuccess();
            } catch (SQLException e) {
                throw translate(e, "prepare point '" + definition.key() + "'");
            }
            prepared.add(preparedPoint);
        }
    }

    private void createSchema(Context context) throws SQLException, StorageException {
        LedgerSql sql = context.sql();
        createTable(context, AccountEntity.class, sql.accountTable);
        createTable(context, TransactionEntity.class, sql.transactionTable);
        createTable(context, MetaEntity.class, sql.metaTable);
        try (LedgerSession session = open(context)) {
            String version = selectMeta(session, context, SCHEMA_VERSION_KEY);
            if (version == null)
                putMeta(session, context, SCHEMA_VERSION_KEY, String.valueOf(SCHEMA_VERSION));
            else if (Integer.parseInt(version) > SCHEMA_VERSION)
                throw new StorageException("Tables with prefix '" + context.settings().tablePrefix() + "' were upgraded by a newer InfPoints (schema "
                        + version + "), update the plugin");
            session.commit();
        }
    }

    private <T> void createTable(Context context, Class<T> entity, String tableName) throws SQLException {
        ConnectionSource source = context.source();
        if (tableExists(source, tableName))
            return;
        DatabaseTableConfig<T> tableConfig = tableConfig(source.getDatabaseType(), context.sql(), entity, tableName, context.settings().tablePrefix());
        try {
            TableUtils.createTableIfNotExists(source, tableConfig);
            logger.get().info("Created table '{}'", tableName);
        } catch (SQLException e) {
            if (!tableExists(source, tableName))
                throw e;
        }
    }

    private static <T> DatabaseTableConfig<T> tableConfig(DatabaseType databaseType, LedgerSql sql, Class<T> entity, String tableName, String prefix) throws SQLException {
        List<DatabaseFieldConfig> fields = new ArrayList<>();
        for (Field field : entity.getDeclaredFields()) {
            DatabaseFieldConfig fieldConfig = DatabaseFieldConfig.fromField(databaseType, tableName, field);
            if (fieldConfig == null)
                continue;
            fieldConfig.setColumnName(sql.entityName(fieldConfig.getColumnName()));
            // Index names are unique per schema in some databases, so they carry the table prefix too
            String indexName = fieldConfig.getIndexName(tableName);
            if (indexName != null)
                fieldConfig.setIndexName(sql.entityName(prefix + indexName));
            fields.add(fieldConfig);
        }
        return new DatabaseTableConfig<>(entity, tableName, fields);
    }

    private static boolean tableExists(ConnectionSource source, String tableName) throws SQLException {
        DatabaseConnection connection = source.getReadOnlyConnection(tableName);
        try {
            return tableExists(connection.getUnderlyingConnection(), tableName);
        } finally {
            source.releaseConnection(connection);
        }
    }

    // Table types aren't filtered because drivers disagree on them, e.g. H2 2.x reports "BASE TABLE"
    private static boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (ResultSet tables = connection.getMetaData().getTables(connection.getCatalog(), null, "%", null)) {
            while (tables.next()) {
                if (tableName.equalsIgnoreCase(tables.getString("TABLE_NAME")))
                    return true;
            }
        }
        return false;
    }

    private void checkDecimals(LedgerSession session, Context context, PointDefinition definition) throws SQLException, StorageException {
        String key = DECIMALS_KEY + definition.key();
        String stored = selectMeta(session, context, key);
        if (stored == null) {
            putMeta(session, context, key, String.valueOf(definition.decimals()));
            session.commit();
            return;
        }
        session.commit();
        if (Integer.parseInt(stored) != definition.decimals())
            throw new StorageException("Point '" + definition.key() + "' stores balances with " + stored + " decimals but is configured with "
                    + definition.decimals() + ", set 'decimals: " + stored + "' back");
    }

    private void importLegacyTable(LedgerSession session, Context context, PointDefinition definition) throws SQLException {
        String marker = LEGACY_TABLE_KEY + definition.key();
        boolean done = selectMeta(session, context, marker) != null;
        session.commit();
        if (done)
            return;
        LedgerSql sql = context.sql();
        String legacyTable = definition.key();
        boolean ownTable = legacyTable.equalsIgnoreCase(sql.accountTable) || legacyTable.equalsIgnoreCase(sql.transactionTable)
                || legacyTable.equalsIgnoreCase(sql.metaTable);
        String from = " FROM " + sql.escape(legacyTable);
        String columns = "SELECT " + sql.escape("player") + ", " + sql.escape("balance");
        if (ownTable || !tableExists(session.connection(), legacyTable) || !queries(session, columns + from + " WHERE 1 = 0")) {
            putMeta(session, context, marker, "none");
            session.commit();
            return;
        }

        logger.get().info("Importing balances of point '{}' from the InfPoints 2.x table '{}'", definition.key(), legacyTable);
        String select = columns + from + " ORDER BY " + sql.escape("player") + " LIMIT ? OFFSET ?";
        ImportCount count = new ImportCount();
        int offset = 0;
        while (true) {
            List<LegacyBalance> batch = new ArrayList<>();
            try (PreparedStatement statement = session.prepare(select)) {
                statement.setInt(1, IMPORT_BATCH_SIZE);
                statement.setInt(2, offset);
                try (ResultSet results = statement.executeQuery()) {
                    while (results.next())
                        batch.add(new LegacyBalance(results.getString(1), results.getDouble(2)));
                }
            }
            session.commit();
            if (batch.isEmpty())
                break;
            offset += batch.size();
            importBatch(session, context, definition, batch, "migration:sql:", "Imported from InfPoints 2.x table '" + legacyTable + "'", count);
            if (batch.size() < IMPORT_BATCH_SIZE)
                break;
        }
        putMeta(session, context, marker, String.valueOf(count.imported));
        session.commit();
        logger.get().info("Imported {} balances of point '{}' from table '{}' ({} skipped, {} invalid, rounding difference {}). "
                        + "The old table is kept, drop it once the balances are verified",
                count.imported, definition.key(), legacyTable, count.skipped, count.invalid, count.rounding.toPlainString());
    }

    private void importLegacyJson(LedgerSession session, Context context, PointDefinition definition) throws SQLException, StorageException {
        Map<String, Double> balances;
        try {
            balances = legacyImports.jsonBalances(definition.key());
        } catch (IOException | RuntimeException e) {
            throw new StorageException("Couldn't read the InfPoints 2.x JSON balances of point '" + definition.key() + "'", e);
        }
        if (balances == null)
            return;
        String marker = LEGACY_JSON_KEY + definition.key();
        boolean done = selectMeta(session, context, marker) != null;
        session.commit();
        if (done) {
            legacyImports.jsonImported(definition.key());
            return;
        }

        logger.get().info("Importing {} balances of point '{}' from the InfPoints 2.x JSON file", balances.size(), definition.key());
        ImportCount count = new ImportCount();
        List<LegacyBalance> batch = new ArrayList<>();
        for (Map.Entry<String, Double> balance : balances.entrySet()) {
            batch.add(new LegacyBalance(balance.getKey(), balance.getValue() == null ? Double.NaN : balance.getValue()));
            if (batch.size() == IMPORT_BATCH_SIZE) {
                importBatch(session, context, definition, batch, "migration:json:", "Imported from InfPoints 2.x file 'data/" + definition.key() + ".json'", count);
                batch.clear();
            }
        }
        if (!batch.isEmpty())
            importBatch(session, context, definition, batch, "migration:json:", "Imported from InfPoints 2.x file 'data/" + definition.key() + ".json'", count);
        putMeta(session, context, marker, String.valueOf(count.imported));
        session.commit();
        legacyImports.jsonImported(definition.key());
        logger.get().info("Imported {} balances of point '{}' from its JSON file ({} skipped, {} invalid, rounding difference {})",
                count.imported, definition.key(), count.skipped, count.invalid, count.rounding.toPlainString());
    }

    private void importBatch(LedgerSession session, Context context, PointDefinition definition, List<LegacyBalance> batch,
                             String keyPrefix, String reason, ImportCount total) throws SQLException {
        ImportCount count = new ImportCount();
        try {
            for (LegacyBalance balance : batch)
                importBalance(session, context, definition, balance, keyPrefix, reason, count);
            session.commit();
        } catch (SQLException e) {
            session.rollback();
            // Another server may be importing the same data, so fall back to one transaction per balance
            count = new ImportCount();
            for (LegacyBalance balance : batch) {
                try {
                    importBalance(session, context, definition, balance, keyPrefix, reason, count);
                    session.commit();
                } catch (SQLException rowException) {
                    session.rollback();
                    UUID player = UUID.fromString(balance.player());
                    boolean exists = selectBalance(session, context, definition.key(), player).isPresent();
                    session.commit();
                    if (!exists)
                        throw rowException;
                    count.skipped++;
                }
            }
        }
        total.imported += count.imported;
        total.skipped += count.skipped;
        total.invalid += count.invalid;
        total.rounding = total.rounding.add(count.rounding);
    }

    private void importBalance(LedgerSession session, Context context, PointDefinition definition, LegacyBalance legacy,
                               String keyPrefix, String reason, ImportCount count) throws SQLException {
        UUID player;
        long units;
        try {
            player = UUID.fromString(legacy.player());
            units = definition.toUnits(legacy.balance());
        } catch (IllegalArgumentException | ArithmeticException e) {
            logger.get().warn("Skipped legacy balance {} of '{}' for point '{}': {}", legacy.balance(), legacy.player(), definition.key(), e.getMessage());
            count.invalid++;
            return;
        }
        if (selectBalance(session, context, definition.key(), player).isPresent()) {
            logger.get().warn("Skipped legacy balance {} of {} for point '{}': the player already has a balance", legacy.balance(), player, definition.key());
            count.skipped++;
            return;
        }
        long now = System.currentTimeMillis();
        insertAccount(session, context, definition.key(), player, units, now);
        insertEntry(session, context, definition.key(), player, TransactionType.MIGRATION, units, units, null, keyPrefix + player,
                INTERNAL_SOURCE, null, reason, now);
        count.imported++;
        count.rounding = count.rounding.add(BigDecimal.valueOf(legacy.balance()).subtract(Amounts.toDecimal(units, definition.decimals())));
    }

    private static boolean queries(LedgerSession session, String sql) {
        try (PreparedStatement statement = session.prepare(sql)) {
            statement.executeQuery().close();
            session.commit();
            return true;
        } catch (SQLException e) {
            session.rollback();
            return false;
        }
    }

    private static int update(LedgerSession session, String sql, long units, long now, String point, UUID player, long limit) throws SQLException {
        try (PreparedStatement statement = session.prepare(sql)) {
            statement.setLong(1, units);
            statement.setLong(2, now);
            statement.setString(3, point);
            statement.setString(4, player.toString());
            statement.setLong(5, limit);
            return statement.executeUpdate();
        }
    }

    private static OptionalLong selectBalance(LedgerSession session, Context context, String point, UUID player) throws SQLException {
        try (PreparedStatement statement = session.prepare(context.sql().selectBalance)) {
            statement.setString(1, point);
            statement.setString(2, player.toString());
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? OptionalLong.of(results.getLong(1)) : OptionalLong.empty();
            }
        }
    }

    private static long requireBalance(LedgerSession session, Context context, String point, UUID player) throws SQLException {
        return selectBalance(session, context, point, player)
                .orElseThrow(() -> new SQLException("Account of " + player + " for point '" + point + "' doesn't exist"));
    }

    private static void insertAccount(LedgerSession session, Context context, String point, UUID player, long balance, long now) throws SQLException {
        try (PreparedStatement statement = session.prepare(context.sql().insertAccount)) {
            statement.setString(1, point);
            statement.setString(2, player.toString());
            statement.setLong(3, balance);
            statement.setLong(4, now);
            statement.executeUpdate();
        }
    }

    private static LedgerEntry insertEntry(LedgerSession session, Context context, String point, UUID player, TransactionType type, long units,
                                           long balance, @Nullable UUID correlation, @Nullable String idempotencyKey, Mutation mutation, long now) throws SQLException {
        return insertEntry(session, context, point, player, type, units, balance, correlation, idempotencyKey,
                mutation.source(), mutation.actor(), mutation.reason(), now);
    }

    private static LedgerEntry insertEntry(LedgerSession session, Context context, String point, UUID player, TransactionType type, long units,
                                           long balance, @Nullable UUID correlation, @Nullable String idempotencyKey, @Nullable String source,
                                           @Nullable UUID actor, @Nullable String reason, long now) throws SQLException {
        String server = truncate(context.settings().serverName(), 64);
        String truncatedSource = truncate(source, 64);
        String truncatedReason = truncate(reason, 255);
        try (PreparedStatement statement = session.prepareInsert(context.sql().insertEntry)) {
            statement.setString(1, point);
            statement.setString(2, player.toString());
            statement.setString(3, type.name());
            statement.setLong(4, units);
            statement.setLong(5, balance);
            setNullable(statement, 6, correlation == null ? null : correlation.toString());
            setNullable(statement, 7, idempotencyKey);
            setNullable(statement, 8, truncatedSource);
            setNullable(statement, 9, actor == null ? null : actor.toString());
            setNullable(statement, 10, truncatedReason);
            setNullable(statement, 11, server);
            statement.setLong(12, now);
            statement.executeUpdate();
            long id = 0;
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next())
                    id = keys.getLong(1);
            }
            return new LedgerEntry(id, point, player, type, units, balance, correlation, idempotencyKey, truncatedSource, actor,
                    truncatedReason, server, Instant.ofEpochMilli(now));
        }
    }

    private static List<LedgerEntry> selectByIdempotencyKey(LedgerSession session, Context context, String point, String idempotencyKey) throws SQLException {
        List<LedgerEntry> entries;
        try (PreparedStatement statement = session.prepare(context.sql().selectByIdempotencyKey)) {
            statement.setString(1, point);
            statement.setString(2, idempotencyKey);
            entries = readEntries(statement);
        }
        if (entries.isEmpty() || entries.get(0).correlationId() == null)
            return entries;
        try (PreparedStatement statement = session.prepare(context.sql().selectByCorrelation)) {
            statement.setString(1, point);
            statement.setString(2, entries.get(0).correlationId().toString());
            return readEntries(statement);
        }
    }

    private static List<LedgerEntry> readEntries(PreparedStatement statement) throws SQLException {
        List<LedgerEntry> entries = new ArrayList<>();
        try (ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                entries.add(new LedgerEntry(
                        results.getLong(1),
                        results.getString(2),
                        UUID.fromString(results.getString(3)),
                        TransactionType.valueOf(results.getString(4)),
                        results.getLong(5),
                        results.getLong(6),
                        uuidOrNull(results.getString(7)),
                        results.getString(8),
                        results.getString(9),
                        uuidOrNull(results.getString(10)),
                        results.getString(11),
                        results.getString(12),
                        Instant.ofEpochMilli(results.getLong(13))
                ));
            }
        }
        return entries;
    }

    private static @Nullable String selectMeta(LedgerSession session, Context context, String key) throws SQLException {
        try (PreparedStatement statement = session.prepare(context.sql().selectMeta)) {
            statement.setString(1, key);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? results.getString(1) : null;
            }
        }
    }

    private static void putMeta(LedgerSession session, Context context, String key, String value) throws SQLException {
        try (PreparedStatement statement = session.prepare(context.sql().updateMeta)) {
            statement.setString(1, value);
            statement.setString(2, key);
            if (statement.executeUpdate() > 0)
                return;
        }
        try (PreparedStatement statement = session.prepare(context.sql().insertMeta)) {
            statement.setString(1, key);
            statement.setString(2, value);
            statement.executeUpdate();
        }
    }

    private static void setNullable(PreparedStatement statement, int index, @Nullable String value) throws SQLException {
        if (value == null)
            statement.setNull(index, Types.VARCHAR);
        else
            statement.setString(index, value);
    }

    private static @Nullable String truncate(@Nullable String value, int length) {
        return value == null || value.length() <= length ? value : value.substring(0, length);
    }

    private static @Nullable UUID uuidOrNull(@Nullable String value) {
        return value == null ? null : UUID.fromString(value);
    }

    private Context context() throws StorageUnavailableException {
        ConnectionSource source = sources.get();
        if (source == null)
            throw new StorageUnavailableException("ConnectionSource has no active connection pool");
        LedgerSettings current = settings.get();
        Context cached = context;
        if (cached != null && cached.source() == source && cached.settings().equals(current))
            return cached;
        Context created = new Context(source, current, new LedgerSql(source.getDatabaseType(), current.tablePrefix()));
        context = created;
        return created;
    }

    private LedgerSession open(Context context) throws SQLException, StorageUnavailableException {
        breaker.check();
        LedgerSettings settings = context.settings();
        int waitMillis = mainThread.getAsBoolean()
                ? settings.syncWaitMillis()
                : Math.max(settings.syncWaitMillis(), (int) TimeUnit.SECONDS.toMillis(settings.queryTimeoutSeconds()));
        DatabaseConnection connection = acquireBounded(context, context.sql().transactionTable, waitMillis);
        try {
            return new LedgerSession(context.source(), connection, context.settings().queryTimeoutSeconds());
        } catch (SQLException e) {
            context.source().releaseConnection(connection);
            throw e;
        }
    }

    // Nothing waits for the pool's own connection timeout (30 seconds in ConnectionSource): the connection is acquired
    // on a helper thread, and a caller that gives up hands a late connection back to the pool
    private DatabaseConnection acquireBounded(Context context, String table, int waitMillis) throws SQLException, StorageUnavailableException {
        Future<DatabaseConnection> future;
        try {
            future = connectionExecutor.submit(() -> context.source().getReadWriteConnection(table));
        } catch (RuntimeException e) {
            throw new StorageUnavailableException("InfPoints is shutting down");
        }
        waitMillis = Math.max(1, waitMillis);
        try {
            return future.get(waitMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            releaseWhenAcquired(context.source(), future);
            if (breaker.recordFailure())
                logConnectionPause();
            throw new StorageUnavailableException("Timed out after " + waitMillis + " ms waiting for a database connection");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            releaseWhenAcquired(context.source(), future);
            throw new StorageUnavailableException("Interrupted while waiting for a database connection");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof SQLException sqlException)
                throw sqlException;
            throw new SQLException("Couldn't acquire a database connection", e.getCause());
        }
    }

    private void releaseWhenAcquired(ConnectionSource source, Future<DatabaseConnection> future) {
        try {
            connectionExecutor.execute(() -> {
                try {
                    source.releaseConnection(future.get());
                } catch (Exception ignored) {
                }
            });
        } catch (RuntimeException ignored) {
        }
    }

    private StorageException translate(SQLException exception, String action) {
        if (isConnectionFailure(exception)) {
            if (breaker.recordFailure())
                logConnectionPause();
            return new StorageUnavailableException("Couldn't " + action + ": " + exception.getMessage(), exception);
        }
        return new StorageException("Couldn't " + action, exception);
    }

    private void logConnectionPause() {
        logger.get().warn("Database connections keep failing, InfPoints pauses database access for {} seconds", breaker.cooldownSeconds());
    }

    private static boolean isConnectionFailure(SQLException exception) {
        String state = exception.getSQLState();
        return exception instanceof SQLTransientConnectionException
                || exception instanceof SQLNonTransientConnectionException
                || (state != null && state.startsWith("08"));
    }

    private static boolean isRetryable(SQLException exception) {
        String state = exception.getSQLState();
        return exception instanceof SQLTransactionRollbackException
                || (state != null && state.startsWith("40"))
                || exception.getErrorCode() == H2_LOCK_TIMEOUT;
    }

    private static void backoff(int attempt) throws StorageException {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(5, 20) * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StorageException("Interrupted while retrying a transaction");
        }
    }

}
