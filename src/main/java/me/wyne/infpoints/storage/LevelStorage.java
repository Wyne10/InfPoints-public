package me.wyne.infpoints.storage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.wyne.infpoints.api.transaction.TransactionResult.Status;
import me.wyne.infpoints.api.transaction.TransactionType;
import me.wyne.infpoints.point.PointDefinition;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

@Singleton
public final class LevelStorage implements PointStorage {

    private final ExperienceChanges changes;

    @Inject
    public LevelStorage(ExperienceChanges changes) {
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
        return PlayerOfflineException.requireOnline(player).getLevel();
    }

    @Override
    public @NotNull StorageResult apply(@NotNull PointDefinition definition, @NotNull Mutation mutation) throws StorageException {
        Player player = PlayerOfflineException.requireOnline(mutation.player());
        long current = player.getLevel();
        long units = mutation.units();
        switch (mutation.operation()) {
            case ADD -> {
                if (current + units > Integer.MAX_VALUE)
                    return StorageResult.failure(Status.FAILED, current);
                setLevel(player, current + units);
                return StorageResult.success(current + units, List.of(
                        LedgerEntry.unrecorded(definition.key(), player.getUniqueId(), TransactionType.ADD, units, current + units, null, mutation)));
            }
            case SUBTRACT -> {
                if (current < units)
                    return StorageResult.failure(Status.INSUFFICIENT_FUNDS, current);
                setLevel(player, current - units);
                return StorageResult.success(current - units, List.of(
                        LedgerEntry.unrecorded(definition.key(), player.getUniqueId(), TransactionType.SUBTRACT, -units, current - units, null, mutation)));
            }
            case SET -> {
                if (units > Integer.MAX_VALUE)
                    return StorageResult.failure(Status.FAILED, current);
                setLevel(player, units);
                return StorageResult.success(units, List.of(
                        LedgerEntry.unrecorded(definition.key(), player.getUniqueId(), TransactionType.SET, units - current, units, null, mutation)));
            }
            case TRANSFER -> {
                Player receiver = PlayerOfflineException.requireOnline(mutation.receiver());
                long received = receiver.getLevel();
                if (current < units)
                    return StorageResult.failure(Status.INSUFFICIENT_FUNDS, current);
                if (received + units > Integer.MAX_VALUE)
                    return StorageResult.failure(Status.FAILED, current);
                setLevel(player, current - units);
                setLevel(receiver, received + units);
                UUID correlation = UUID.randomUUID();
                return StorageResult.success(current - units, List.of(
                        LedgerEntry.unrecorded(definition.key(), player.getUniqueId(), TransactionType.TRANSFER_OUT, -units, current - units, correlation, mutation),
                        LedgerEntry.unrecorded(definition.key(), receiver.getUniqueId(), TransactionType.TRANSFER_IN, units, received + units, correlation,
                                mutation.withoutIdempotencyKey())));
            }
        }
        throw new StorageException("Unknown operation " + mutation.operation());
    }

    private void setLevel(Player player, long level) {
        changes.expect(player.getUniqueId(), (int) level);
        player.setLevel((int) level);
    }


}
