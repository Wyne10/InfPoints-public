package org.bigcraft.infpoints.storage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.wyne.wutils.common.player.PlayerUtils;
import org.bigcraft.infpoints.api.transaction.TransactionResult.Status;
import org.bigcraft.infpoints.api.transaction.TransactionType;
import org.bigcraft.infpoints.point.PointDefinition;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

@Singleton
public final class ExpStorage implements PointStorage {

    private final ExperienceChanges changes;

    @Inject
    public ExpStorage(ExperienceChanges changes) {
        this.changes = changes;
    }

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

    @Override
    public long balance(@NotNull PointDefinition definition, @NotNull UUID player) throws StorageException {
        return PlayerUtils.currentExp(PlayerOfflineException.requireOnline(player));
    }

    @Override
    public @NotNull StorageResult apply(@NotNull PointDefinition definition, @NotNull Mutation mutation) throws StorageException {
        Player player = PlayerOfflineException.requireOnline(mutation.player());
        long current = PlayerUtils.currentExp(player);
        long units = mutation.units();
        switch (mutation.operation()) {
            case ADD -> {
                if (units > Integer.MAX_VALUE || current + units > Integer.MAX_VALUE)
                    return StorageResult.failure(Status.FAILED, current);
                // Giving experience instead of resetting it avoids a level up sound on every addition
                player.giveExp((int) units);
                changes.expect(player.getUniqueId(), player.getLevel());
                long updated = PlayerUtils.currentExp(player);
                return StorageResult.success(updated, List.of(
                        LedgerEntry.unrecorded(definition.key(), player.getUniqueId(), TransactionType.ADD, updated - current, updated, null, mutation)));
            }
            case SUBTRACT -> {
                if (current < units)
                    return StorageResult.failure(Status.INSUFFICIENT_FUNDS, current);
                long updated = setTotal(player, current - units);
                return StorageResult.success(updated, List.of(
                        LedgerEntry.unrecorded(definition.key(), player.getUniqueId(), TransactionType.SUBTRACT, updated - current, updated, null, mutation)));
            }
            case SET -> {
                if (units > Integer.MAX_VALUE)
                    return StorageResult.failure(Status.FAILED, current);
                long updated = setTotal(player, units);
                return StorageResult.success(updated, List.of(
                        LedgerEntry.unrecorded(definition.key(), player.getUniqueId(), TransactionType.SET, updated - current, updated, null, mutation)));
            }
            case TRANSFER -> {
                Player receiver = PlayerOfflineException.requireOnline(mutation.receiver());
                long received = PlayerUtils.currentExp(receiver);
                if (current < units)
                    return StorageResult.failure(Status.INSUFFICIENT_FUNDS, current);
                if (received + units > Integer.MAX_VALUE)
                    return StorageResult.failure(Status.FAILED, current);
                long debited = setTotal(player, current - units);
                long credited = setTotal(receiver, received + units);
                UUID correlation = UUID.randomUUID();
                return StorageResult.success(debited, List.of(
                        LedgerEntry.unrecorded(definition.key(), player.getUniqueId(), TransactionType.TRANSFER_OUT, debited - current, debited, correlation, mutation),
                        LedgerEntry.unrecorded(definition.key(), receiver.getUniqueId(), TransactionType.TRANSFER_IN, credited - received, credited, correlation,
                                mutation.withoutIdempotencyKey())));
            }
        }
        throw new StorageException("Unknown operation " + mutation.operation());
    }

    private long setTotal(Player player, long total) {
        PlayerUtils.setExp(player, (int) total);
        changes.expect(player.getUniqueId(), player.getLevel());
        return PlayerUtils.currentExp(player);
    }

}
