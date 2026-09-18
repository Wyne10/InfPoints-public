package org.bigcraft.infpoints.storage;

import com.google.inject.Singleton;
import org.bigcraft.infpoints.InfPoints;
import org.bigcraft.infpoints.api.transaction.TransactionResult.Status;
import org.bigcraft.infpoints.api.transaction.TransactionType;
import org.bigcraft.infpoints.point.Amounts;
import org.bigcraft.infpoints.point.PointDefinition;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

@Singleton
public final class PdcStorage implements PointStorage {

    @Override
    public @NotNull ThreadPolicy threadPolicy() {
        return ThreadPolicy.MAIN;
    }

    @Override
    public boolean supportsHistory() {
        return false;
    }

    @Override
    public boolean supportsOfflinePlayers() {
        return false;
    }

    public @Nullable String validate(@NotNull PointDefinition definition) {
        return key(definition) == null
                ? "its key can't be stored in persistent data, use lowercase letters, digits and '-' only"
                : null;
    }

    @Override
    public long balance(@NotNull PointDefinition definition, @NotNull UUID player) throws StorageException {
        return read(PlayerOfflineException.requireOnline(player), definition);
    }

    @Override
    public @NotNull StorageResult apply(@NotNull PointDefinition definition, @NotNull Mutation mutation) throws StorageException {
        Player player = PlayerOfflineException.requireOnline(mutation.player());
        long current = read(player, definition);
        switch (mutation.operation()) {
            case ADD -> {
                long updated;
                try {
                    updated = Math.addExact(current, mutation.units());
                } catch (ArithmeticException e) {
                    return StorageResult.failure(Status.FAILED, current);
                }
                write(player, definition, updated);
                return StorageResult.success(updated, List.of(
                        LedgerEntry.unrecorded(definition.key(), player.getUniqueId(), TransactionType.ADD, mutation.units(), updated, null, mutation)));
            }
            case SUBTRACT -> {
                if (current < mutation.units())
                    return StorageResult.failure(Status.INSUFFICIENT_FUNDS, current);
                long updated = current - mutation.units();
                write(player, definition, updated);
                return StorageResult.success(updated, List.of(
                        LedgerEntry.unrecorded(definition.key(), player.getUniqueId(), TransactionType.SUBTRACT, -mutation.units(), updated, null, mutation)));
            }
            case SET -> {
                long delta;
                try {
                    delta = Math.subtractExact(mutation.units(), current);
                } catch (ArithmeticException e) {
                    return StorageResult.failure(Status.FAILED, current);
                }
                write(player, definition, mutation.units());
                return StorageResult.success(mutation.units(), List.of(
                        LedgerEntry.unrecorded(definition.key(), player.getUniqueId(), TransactionType.SET, delta, mutation.units(), null, mutation)));
            }
            case TRANSFER -> {
                Player receiver = PlayerOfflineException.requireOnline(mutation.receiver());
                if (current < mutation.units())
                    return StorageResult.failure(Status.INSUFFICIENT_FUNDS, current);
                long credited;
                try {
                    credited = Math.addExact(read(receiver, definition), mutation.units());
                } catch (ArithmeticException e) {
                    return StorageResult.failure(Status.FAILED, current);
                }
                long debited = current - mutation.units();
                write(player, definition, debited);
                write(receiver, definition, credited);
                UUID correlation = UUID.randomUUID();
                return StorageResult.success(debited, List.of(
                        LedgerEntry.unrecorded(definition.key(), player.getUniqueId(), TransactionType.TRANSFER_OUT, -mutation.units(), debited, correlation, mutation),
                        LedgerEntry.unrecorded(definition.key(), receiver.getUniqueId(), TransactionType.TRANSFER_IN, mutation.units(), credited, correlation,
                                mutation.withoutIdempotencyKey())));
            }
        }
        throw new StorageException("Unknown operation " + mutation.operation());
    }

    private long read(Player player, PointDefinition definition) throws StorageException {
        NamespacedKey key = requireKey(definition);
        NamespacedKey decimalsKey = decimalsKey(key);
        PersistentDataContainer data = player.getPersistentDataContainer();
        try {
            // 2.x stored balances as doubles under the same key
            if (data.has(key, PersistentDataType.DOUBLE)) {
                Double legacy = data.get(key, PersistentDataType.DOUBLE);
                long units = definition.toUnits(legacy == null ? 0 : legacy);
                write(player, definition, units);
                InfPoints.logger().info("Converted the 2.x balance {} of {} for point '{}'", legacy, player.getName(), definition.key());
                return units;
            }
            Long stored = data.get(key, PersistentDataType.LONG);
            if (stored == null)
                return definition.defaultUnits();
            Integer storedDecimals = data.get(decimalsKey, PersistentDataType.INTEGER);
            if (storedDecimals == null || storedDecimals == definition.decimals())
                return stored;
            long rescaled = Amounts.rescale(stored, storedDecimals, definition.decimals());
            if (Amounts.rescale(rescaled, definition.decimals(), storedDecimals) != stored)
                InfPoints.logger().warn("Balance of {} for point '{}' lost precision converting from {} to {} decimals", player.getName(), definition.key(),
                        storedDecimals, definition.decimals());
            write(player, definition, rescaled);
            return rescaled;
        } catch (ArithmeticException | IllegalArgumentException e) {
            throw new StorageException("Stored balance of " + player.getName() + " for point '" + definition.key() + "' can't be read", e);
        }
    }

    private void write(Player player, PointDefinition definition, long units) throws StorageException {
        NamespacedKey key = requireKey(definition);
        PersistentDataContainer data = player.getPersistentDataContainer();
        data.set(key, PersistentDataType.LONG, units);
        data.set(decimalsKey(key), PersistentDataType.INTEGER, definition.decimals());
    }

    private static @Nullable NamespacedKey key(PointDefinition definition) {
        return NamespacedKey.fromString("points:" + definition.key());
    }

    private static NamespacedKey requireKey(PointDefinition definition) throws StorageException {
        NamespacedKey key = key(definition);
        if (key == null)
            throw new StorageException("Point '" + definition.key() + "' can't be stored in persistent data");
        return key;
    }

    @SuppressWarnings("deprecation")
    private static NamespacedKey decimalsKey(NamespacedKey key) {
        return new NamespacedKey(key.getNamespace(), key.getKey() + ".decimals");
    }


}
