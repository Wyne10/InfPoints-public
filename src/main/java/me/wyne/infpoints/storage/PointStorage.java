package me.wyne.infpoints.storage;

import me.wyne.infpoints.point.PointDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PointStorage {

    @NotNull ThreadPolicy threadPolicy();

    boolean supportsHistory();

    boolean supportsOfflinePlayers();

    default boolean supportsIdempotency() {
        return supportsHistory();
    }

    // Balances living outside this server are read through the cache and refreshed in the background
    default boolean isRemote() {
        return false;
    }

    long balance(@NotNull PointDefinition definition, @NotNull UUID player) throws StorageException;

    // Mutations arrive validated; implementations change every balance involved or none of them
    @NotNull StorageResult apply(@NotNull PointDefinition definition, @NotNull Mutation mutation) throws StorageException;

    default @NotNull List<LedgerEntry> history(@NotNull PointDefinition definition, @NotNull UUID player, int offset, int limit) throws StorageException {
        return List.of();
    }

    default @NotNull Optional<LedgerEntry> transaction(@NotNull PointDefinition definition, long id) throws StorageException {
        return Optional.empty();
    }

}
