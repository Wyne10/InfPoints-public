package org.bigcraft.infpoints.storage.sql;

import com.google.inject.Singleton;
import org.bigcraft.infpoints.point.PointDefinition;
import org.bigcraft.infpoints.storage.AuditReport;
import org.bigcraft.infpoints.storage.Mutation;
import org.bigcraft.infpoints.storage.SqlStorage;
import org.bigcraft.infpoints.storage.StorageResult;
import org.bigcraft.infpoints.storage.StorageUnavailableException;
import org.bigcraft.infpoints.storage.ThreadPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

// Bound when ConnectionSource is missing; must not reference ORMLite or ConnectionSource classes
@Singleton
public final class UnavailableSqlStorage implements SqlStorage {

    private static final String REASON = "ConnectionSource is not installed or didn't provide a connection pool";

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
    public String prepare(@NotNull PointDefinition definition) {
        return REASON;
    }

    @Override
    public long balance(@NotNull PointDefinition definition, @NotNull UUID player) throws StorageUnavailableException {
        throw new StorageUnavailableException(REASON);
    }

    @Override
    public @NotNull StorageResult apply(@NotNull PointDefinition definition, @NotNull Mutation mutation) throws StorageUnavailableException {
        throw new StorageUnavailableException(REASON);
    }

    @Override
    public @NotNull Map<UUID, Long> balances(@NotNull PointDefinition definition, int offset, int limit) throws StorageUnavailableException {
        throw new StorageUnavailableException(REASON);
    }

    @Override
    public @NotNull AuditReport audit(@NotNull PointDefinition definition, @Nullable UUID player) throws StorageUnavailableException {
        throw new StorageUnavailableException(REASON);
    }

}
