package me.wyne.infpoints.storage;

import me.wyne.infpoints.point.PointDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public interface SqlStorage extends PointStorage {

    // Returns why the point can't be used; an unreachable database isn't one, preparation is retried on next use
    @Nullable String prepare(@NotNull PointDefinition definition);

    // Reads stored balances in pages, ordered by player, for warming up the cache
    @NotNull Map<UUID, Long> balances(@NotNull PointDefinition definition, int offset, int limit) throws StorageException;

    @NotNull AuditReport audit(@NotNull PointDefinition definition, @Nullable UUID player) throws StorageException;

}
