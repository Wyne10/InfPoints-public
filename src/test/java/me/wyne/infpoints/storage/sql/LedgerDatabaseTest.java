package me.wyne.infpoints.storage.sql;

import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import me.wyne.infpoints.api.PointStorageType;
import me.wyne.infpoints.api.transaction.TransactionResult.Status;
import me.wyne.infpoints.api.transaction.TransactionType;
import me.wyne.infpoints.point.PointDefinition;
import me.wyne.infpoints.storage.AuditReport;
import me.wyne.infpoints.storage.LedgerEntry;
import me.wyne.infpoints.storage.Mutation;
import me.wyne.infpoints.storage.StorageException;
import me.wyne.infpoints.storage.StorageResult;
import me.wyne.infpoints.storage.StorageUnavailableException;
import org.h2.jdbcx.JdbcConnectionPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static me.wyne.infpoints.point.TestPoints.add;
import static me.wyne.infpoints.point.TestPoints.definition;
import static me.wyne.infpoints.point.TestPoints.set;
import static me.wyne.infpoints.point.TestPoints.subtract;
import static me.wyne.infpoints.point.TestPoints.transfer;
import static me.wyne.infpoints.point.TestPoints.withKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LedgerDatabaseTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LedgerDatabaseTest.class);
    private static final LedgerSettings SETTINGS = new LedgerSettings("infpoints_", "test-server", 10, 1000);
    private static final AtomicInteger DATABASES = new AtomicInteger();

    private final List<AutoCloseable> resources = new ArrayList<>();
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    @AfterEach
    void closeResources() throws Exception {
        for (int i = resources.size() - 1; i >= 0; i--)
            resources.get(i).close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ";MODE=MySQL"})
    void appliesOperationsAndKeepsLedgerConsistent(String mode) throws Exception {
        DataSourceConnectionSource source = database(mode);
        LedgerDatabase ledger = ledger(source, LegacyImports.NONE);
        PointDefinition point = definition("coins", PointStorageType.SQL, 100, 2);

        assertEquals(10000, ledger.balance(point, alice));
        assertEquals(10500, ledger.apply(point, add(alice, 500)).balance());
        StorageResult insufficient = ledger.apply(point, subtract(alice, 20000));
        assertEquals(Status.INSUFFICIENT_FUNDS, insufficient.status());
        assertEquals(10500, insufficient.balance());
        assertEquals(5500, ledger.apply(point, subtract(alice, 5000)).balance());
        StorageResult set = ledger.apply(point, set(alice, 100));
        assertEquals(-5400, set.entries().get(0).units());
        StorageResult transferred = ledger.apply(point, transfer(alice, bob, 50));
        assertEquals(Status.SUCCESS, transferred.status());
        assertEquals(50, transferred.balance());
        assertEquals(10050, ledger.balance(point, bob));

        List<LedgerEntry> history = ledger.history(point, alice, 0, 10);
        assertEquals(List.of(TransactionType.TRANSFER_OUT, TransactionType.SET, TransactionType.SUBTRACT, TransactionType.ADD, TransactionType.INITIAL),
                history.stream().map(LedgerEntry::type).toList());
        assertEquals("test-server", history.get(0).server());
        assertEquals(history.get(1).id(), ledger.transaction(point, history.get(1).id()).orElseThrow().id());
        assertEquals(2, ledger.history(point, alice, 3, 10).size());

        AuditReport audit = ledger.audit(point, null);
        assertEquals(2, audit.checked());
        assertTrue(audit.mismatches().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ";MODE=MySQL"})
    void concurrentSubtractsNeverOverdraw(String mode) throws Exception {
        DataSourceConnectionSource source = database(mode);
        LedgerDatabase ledger = ledger(source, LegacyImports.NONE);
        PointDefinition point = definition("gems", PointStorageType.SQL, 8000, 0);
        int threads = 16;
        int operations = 1000;

        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int thread = 0; thread < threads; thread++) {
            tasks.add(() -> {
                int applied = 0;
                for (int i = 0; i < operations; i++) {
                    if (ledger.apply(point, subtract(alice, 1)).status() == Status.SUCCESS)
                        applied++;
                }
                return applied;
            });
        }
        int applied = 0;
        for (Future<Integer> future : runAll(tasks))
            applied += future.get();

        assertEquals(8000, applied);
        assertEquals(0, ledger.balance(point, alice));
        assertTrue(ledger.audit(point, alice).mismatches().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ";MODE=MySQL"})
    void concurrentOppositeTransfersStayBalanced(String mode) throws Exception {
        DataSourceConnectionSource source = database(mode);
        LedgerDatabase ledger = ledger(source, LegacyImports.NONE);
        PointDefinition point = definition("gold", PointStorageType.SQL, 100, 0);

        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int thread = 0; thread < 8; thread++) {
            boolean forward = thread % 2 == 0;
            tasks.add(() -> {
                for (int i = 0; i < 200; i++)
                    ledger.apply(point, forward ? transfer(alice, bob, 3) : transfer(bob, alice, 3));
                return 0;
            });
        }
        for (Future<Integer> future : runAll(tasks))
            future.get();

        assertEquals(200, ledger.balance(point, alice) + ledger.balance(point, bob));
        assertTrue(ledger.balance(point, alice) >= 0 && ledger.balance(point, bob) >= 0);
        assertTrue(ledger.audit(point, null).mismatches().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ";MODE=MySQL"})
    void failedTransferChangesNothing(String mode) throws Exception {
        DataSourceConnectionSource source = database(mode);
        LedgerDatabase ledger = ledger(source, LegacyImports.NONE);
        PointDefinition point = definition("coins", PointStorageType.SQL, 10, 0);

        assertEquals(Status.INSUFFICIENT_FUNDS, ledger.apply(point, transfer(alice, bob, 11)).status());
        assertEquals(Status.INSUFFICIENT_FUNDS, ledger.apply(point, transfer(bob, alice, 11)).status());
        assertEquals(10, ledger.balance(point, alice));
        assertEquals(10, ledger.balance(point, bob));
        assertEquals(1, ledger.history(point, alice, 0, 10).size());
        assertEquals(1, ledger.history(point, bob, 0, 10).size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ";MODE=MySQL"})
    void idempotencyKeyAppliesOnceUnderConcurrency(String mode) throws Exception {
        DataSourceConnectionSource source = database(mode);
        LedgerDatabase ledger = ledger(source, LegacyImports.NONE);
        PointDefinition point = definition("donate", PointStorageType.SQL, 0, 0);

        Mutation delivery = withKey(add(alice, 100), "order-42");
        List<Callable<Status>> tasks = new ArrayList<>();
        for (int thread = 0; thread < 8; thread++)
            tasks.add(() -> ledger.apply(point, delivery).status());
        int successes = 0;
        for (Future<Status> future : runAll(tasks)) {
            Status status = future.get();
            assertTrue(status == Status.SUCCESS || status == Status.DUPLICATE);
            if (status == Status.SUCCESS)
                successes++;
        }

        assertEquals(1, successes);
        assertEquals(100, ledger.balance(point, alice));
        StorageResult repeated = ledger.apply(point, delivery);
        assertEquals(Status.DUPLICATE, repeated.status());
        assertEquals("order-42", repeated.entries().get(0).idempotencyKey());

        Mutation transferDelivery = withKey(transfer(alice, bob, 40), "transfer-1");
        assertEquals(Status.SUCCESS, ledger.apply(point, transferDelivery).status());
        StorageResult repeatedTransfer = ledger.apply(point, transferDelivery);
        assertEquals(Status.DUPLICATE, repeatedTransfer.status());
        assertEquals(2, repeatedTransfer.entries().size());
        assertEquals(60, ledger.balance(point, alice));
        assertEquals(40, ledger.balance(point, bob));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ";MODE=MySQL"})
    void setWhileBalanceChangesConcurrently(String mode) throws Exception {
        DataSourceConnectionSource source = database(mode);
        LedgerDatabase ledger = ledger(source, LegacyImports.NONE);
        PointDefinition point = definition("tokens", PointStorageType.SQL, 0, 0);

        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int thread = 0; thread < 6; thread++) {
            boolean setter = thread == 0;
            tasks.add(() -> {
                for (int i = 0; i < 300; i++) {
                    Status status = ledger.apply(point, setter ? set(alice, 1000) : add(alice, 1)).status();
                    if (status != Status.SUCCESS)
                        throw new IllegalStateException("Unexpected " + status);
                }
                return 0;
            });
        }
        for (Future<Integer> future : runAll(tasks))
            future.get();

        assertTrue(ledger.audit(point, alice).mismatches().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ";MODE=MySQL"})
    void readsBalancesInPages(String mode) throws Exception {
        DataSourceConnectionSource source = database(mode);
        LedgerDatabase ledger = ledger(source, LegacyImports.NONE);
        PointDefinition point = definition("tokens", PointStorageType.SQL, 0, 0);

        Map<UUID, Long> expected = new HashMap<>();
        for (int i = 1; i <= 25; i++) {
            UUID player = UUID.randomUUID();
            ledger.apply(point, add(player, i));
            expected.put(player, (long) i);
        }

        Map<UUID, Long> read = new HashMap<>();
        for (int offset = 0; ; offset += 10) {
            Map<UUID, Long> page = ledger.balances(point, offset, 10);
            read.putAll(page);
            if (page.size() < 10)
                break;
        }
        assertEquals(expected, read);
        assertEquals(10, ledger.balances(point, 0, 10).size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ";MODE=MySQL"})
    void overflowFailsWithoutChanges(String mode) throws Exception {
        DataSourceConnectionSource source = database(mode);
        LedgerDatabase ledger = ledger(source, LegacyImports.NONE);
        PointDefinition point = definition("huge", PointStorageType.SQL, 0, 0);

        assertEquals(Status.SUCCESS, ledger.apply(point, add(alice, Long.MAX_VALUE - 5)).status());
        StorageResult overflow = ledger.apply(point, add(alice, 10));
        assertEquals(Status.FAILED, overflow.status());
        assertEquals(Long.MAX_VALUE - 5, ledger.balance(point, alice));

        ledger.apply(point, add(bob, 10));
        assertEquals(Status.FAILED, ledger.apply(point, transfer(bob, alice, 10)).status());
        assertEquals(10, ledger.balance(point, bob));
        assertEquals(Long.MAX_VALUE - 5, ledger.balance(point, alice));
        assertTrue(ledger.audit(point, null).mismatches().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ";MODE=MySQL"})
    void importsLegacyTableOnce(String mode) throws Exception {
        DataSourceConnectionSource source = database(mode);
        LedgerSql sql = new LedgerSql(source.getDatabaseType(), SETTINGS.tablePrefix());
        try (Connection connection = source.getReadWriteConnection("primary").getUnderlyingConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE " + sql.escape("primary") + " (" + sql.escape("player") + " VARCHAR(48) PRIMARY KEY, "
                    + sql.escape("balance") + " DOUBLE)");
            statement.executeUpdate("INSERT INTO " + sql.escape("primary") + " VALUES ('" + alice + "', 12.345), ('" + bob + "', 7), ('not-a-uuid', 5)");
        }
        PointDefinition point = definition("primary", PointStorageType.SQL, 100, 2);

        LedgerDatabase ledger = ledger(source, LegacyImports.NONE);
        ledger.prepare(point);
        assertEquals(1235, ledger.balance(point, alice));
        assertEquals(700, ledger.balance(point, bob));
        List<LedgerEntry> history = ledger.history(point, alice, 0, 10);
        assertEquals(1, history.size());
        assertEquals(TransactionType.MIGRATION, history.get(0).type());

        executeUpdate(source, "DELETE FROM " + sql.escape("infpoints_meta") + " WHERE " + sql.escape("meta_key") + " = ?", "migration.legacy-sql.primary");
        LedgerDatabase restarted = ledger(source, LegacyImports.NONE);
        restarted.prepare(point);
        assertEquals(1, restarted.history(point, alice, 0, 10).size());
        assertEquals(1235, restarted.balance(point, alice));
        assertTrue(restarted.audit(point, null).mismatches().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ";MODE=MySQL"})
    void importsLegacyJsonOnce(String mode) throws Exception {
        DataSourceConnectionSource source = database(mode);
        PointDefinition point = definition("shards", PointStorageType.SQL, 0, 1);
        Map<String, Double> balances = new HashMap<>();
        balances.put(alice.toString(), 3.25);
        balances.put(bob.toString(), 10.0);
        AtomicInteger imported = new AtomicInteger();
        LegacyImports imports = new LegacyImports() {
            @Override
            public @Nullable Map<String, Double> jsonBalances(@NotNull String key) {
                return key.equals("shards") ? balances : null;
            }

            @Override
            public void jsonImported(@NotNull String key) {
                imported.incrementAndGet();
            }
        };

        LedgerDatabase ledger = ledger(source, imports);
        ledger.prepare(point);
        assertEquals(33, ledger.balance(point, alice));
        assertEquals(100, ledger.balance(point, bob));
        assertEquals(1, imported.get());

        LedgerDatabase restarted = ledger(source, imports);
        restarted.prepare(point);
        assertEquals(1, restarted.history(point, bob, 0, 10).size());
        assertEquals(2, imported.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ";MODE=MySQL"})
    void refusesChangedDecimals(String mode) throws Exception {
        DataSourceConnectionSource source = database(mode);
        ledger(source, LegacyImports.NONE).prepare(definition("coins", PointStorageType.SQL, 0, 2));
        StorageException exception = assertThrows(StorageException.class,
                () -> ledger(source, LegacyImports.NONE).prepare(definition("coins", PointStorageType.SQL, 0, 0)));
        assertTrue(exception.getMessage().contains("decimals"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ";MODE=MySQL"})
    void auditFindsTamperedBalances(String mode) throws Exception {
        DataSourceConnectionSource source = database(mode);
        LedgerDatabase ledger = ledger(source, LegacyImports.NONE);
        PointDefinition point = definition("coins", PointStorageType.SQL, 5, 0);
        ledger.apply(point, add(alice, 10));
        ledger.apply(point, add(bob, 10));
        LedgerSql sql = new LedgerSql(source.getDatabaseType(), SETTINGS.tablePrefix());
        executeUpdate(source, "UPDATE " + sql.escape("infpoints_account") + " SET " + sql.escape("balance") + " = 1 WHERE "
                + sql.escape("player") + " = ?", bob.toString());

        AuditReport audit = ledger.audit(point, null);
        assertEquals(2, audit.checked());
        assertEquals(1, audit.mismatches().size());
        assertEquals(bob, audit.mismatches().get(0).player());
        assertEquals(1, audit.mismatches().get(0).balance());
        assertEquals(15, audit.mismatches().get(0).ledger());
    }

    @Test
    void missingConnectionPoolIsUnavailable() {
        LedgerDatabase ledger = new LedgerDatabase(() -> null, () -> SETTINGS, () -> false, () -> LOGGER, LegacyImports.NONE);
        resources.add(ledger);
        PointDefinition point = definition("coins", PointStorageType.SQL, 0, 0);
        assertThrows(StorageUnavailableException.class, () -> ledger.apply(point, add(alice, 1)));
    }

    private DataSourceConnectionSource database(String mode) throws SQLException {
        String url = "jdbc:h2:mem:ledger" + DATABASES.incrementAndGet() + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000" + mode;
        JdbcConnectionPool pool = JdbcConnectionPool.create(url, "sa", "");
        pool.setMaxConnections(64);
        resources.add(pool::dispose);
        DataSourceConnectionSource source = new DataSourceConnectionSource(pool, url);
        resources.add(source);
        return source;
    }

    private LedgerDatabase ledger(DataSourceConnectionSource source, LegacyImports imports) {
        LedgerDatabase ledger = new LedgerDatabase(() -> source, () -> SETTINGS, () -> false, () -> LOGGER, imports);
        resources.add(ledger);
        return ledger;
    }

    private static void executeUpdate(DataSourceConnectionSource source, String sql, String parameter) throws SQLException {
        try (Connection connection = source.getReadWriteConnection("any").getUnderlyingConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, parameter);
            statement.executeUpdate();
        }
    }

    private static <T> List<Future<T>> runAll(List<Callable<T>> tasks) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        try {
            List<Future<T>> futures = executor.invokeAll(tasks);
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.MINUTES));
            return futures;
        } finally {
            executor.shutdownNow();
        }
    }

}
