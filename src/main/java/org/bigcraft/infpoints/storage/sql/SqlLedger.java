package org.bigcraft.infpoints.storage.sql;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.j256.ormlite.support.ConnectionSource;
import me.wyne.wutils.common.terminable.Terminable;
import me.wyne.wutils.config.Config;
import me.wyne.wutils.config.ConfigEntry;
import org.bigcraft.connection.api.ConnectionPool;
import org.bigcraft.connection.api.ConnectionProvider;
import org.bigcraft.infpoints.InfPoints;
import org.bigcraft.infpoints.point.PointDefinition;
import org.bigcraft.infpoints.storage.AuditReport;
import org.bigcraft.infpoints.storage.LedgerEntry;
import org.bigcraft.infpoints.storage.Mutation;
import org.bigcraft.infpoints.storage.SqlStorage;
import org.bigcraft.infpoints.storage.StorageException;
import org.bigcraft.infpoints.storage.StorageResult;
import org.bigcraft.infpoints.storage.StorageUnavailableException;
import org.bigcraft.infpoints.storage.ThreadPolicy;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("FieldMayBeFinal")
@Singleton
public final class SqlLedger implements SqlStorage, Terminable {

    @ConfigEntry(section = "Storage", comment = "Prefix of the tables InfPoints creates in the ConnectionSource database")
    private String tablePrefix = "infpoints_";

    @ConfigEntry(section = "Storage", comment = "Name of this server, stored with every transaction so servers sharing a database can be told apart")
    private String serverName = "server";

    @ConfigEntry(section = "Storage", comment = "Seconds a database statement may run before it is cancelled and its transaction rolled back")
    private int queryTimeoutSeconds = 5;

    @ConfigEntry(section = "Storage", comment = "Milliseconds the main thread waits for a free database connection before an operation fails")
    private int syncWaitMillis = 1000;

    private final InfPoints plugin;
    private final ConnectionProvider provider;
    private final LedgerDatabase database;

    @Inject
    public SqlLedger(InfPoints plugin, ConnectionProvider provider) {
        this.plugin = plugin;
        this.provider = provider;
        this.database = new LedgerDatabase(this::source, this::settings, Bukkit::isPrimaryThread, InfPoints::logger, new JsonFiles());
        Config.global.registerConfigObject(this);
        plugin.bind(this);
    }

    @Override
    public @NotNull ThreadPolicy threadPolicy() {
        return ThreadPolicy.ANY;
    }

    @Override
    public boolean supportsHistory() {
        return true;
    }

    @Override
    public boolean supportsOfflinePlayers() {
        return true;
    }

    @Override
    public boolean isRemote() {
        return true;
    }

    @Override
    public @Nullable String prepare(@NotNull PointDefinition definition) {
        try {
            database.prepare(definition);
            return null;
        } catch (StorageUnavailableException e) {
            InfPoints.logger().warn("Database is unavailable, point '{}' will be prepared once it can be reached: {}", definition.key(), e.getMessage());
            return null;
        } catch (StorageException e) {
            if (e.getCause() != null)
                InfPoints.logger().error("Couldn't prepare point '{}'", definition.key(), e);
            return e.getMessage();
        }
    }

    @Override
    public long balance(@NotNull PointDefinition definition, @NotNull UUID player) throws StorageException {
        return database.balance(definition, player);
    }

    @Override
    public @NotNull StorageResult apply(@NotNull PointDefinition definition, @NotNull Mutation mutation) throws StorageException {
        return database.apply(definition, mutation);
    }

    @Override
    public @NotNull Map<UUID, Long> balances(@NotNull PointDefinition definition, int offset, int limit) throws StorageException {
        return database.balances(definition, offset, limit);
    }

    @Override
    public @NotNull List<LedgerEntry> history(@NotNull PointDefinition definition, @NotNull UUID player, int offset, int limit) throws StorageException {
        return database.history(definition, player, offset, limit);
    }

    @Override
    public @NotNull Optional<LedgerEntry> transaction(@NotNull PointDefinition definition, long id) throws StorageException {
        return database.transaction(definition, id);
    }

    @Override
    public @NotNull AuditReport audit(@NotNull PointDefinition definition, @Nullable UUID player) throws StorageException {
        return database.audit(definition, player);
    }

    @Override
    public void close() {
        database.close();
    }

    // Looked up on every use, because reloading ConnectionSource replaces its pool
    private @Nullable ConnectionSource source() {
        if (!provider.isActive())
            return null;
        ConnectionPool<ConnectionSource> pool = provider.getConnectionPool();
        return pool == null ? null : pool.getSource();
    }

    private LedgerSettings settings() {
        return new LedgerSettings(tablePrefix, serverName, queryTimeoutSeconds, syncWaitMillis);
    }

    private final class JsonFiles implements LegacyImports {

        @Override
        public @Nullable Map<String, Double> jsonBalances(@NotNull String point) throws IOException {
            File file = file(point);
            if (!file.isFile())
                return null;
            try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                JsonElement root = new JsonParser().parse(reader);
                Map<String, Double> balances = new LinkedHashMap<>();
                if (root.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject().entrySet())
                        balances.put(entry.getKey(), entry.getValue().isJsonPrimitive() ? entry.getValue().getAsDouble() : Double.NaN);
                }
                return balances;
            } catch (JsonParseException | IllegalStateException | NumberFormatException e) {
                throw new IOException("Invalid JSON in " + file.getPath(), e);
            }
        }

        @Override
        public void jsonImported(@NotNull String point) {
            File file = file(point);
            if (!file.isFile())
                return;
            try {
                Files.move(file.toPath(), file.toPath().resolveSibling(file.getName() + ".migrated"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                InfPoints.logger().error("Imported '{}' but couldn't rename it, rename or remove it manually", file.getPath(), e);
            }
        }

        private File file(String point) {
            return new File(plugin.getDataFolder(), "data/" + point + ".json");
        }

    }

}
