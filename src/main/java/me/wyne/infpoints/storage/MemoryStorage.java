package me.wyne.infpoints.storage;

import me.wyne.infpoints.api.transaction.TransactionResult.Status;
import me.wyne.infpoints.api.transaction.TransactionType;
import me.wyne.infpoints.point.PointDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MemoryStorage implements PointStorage {

    private final Map<UUID, Long> balances = new HashMap<>();
    private final Map<String, List<LedgerEntry>> appliedKeys = new HashMap<>();

    @Override
    public @NotNull ThreadPolicy threadPolicy() {
        return ThreadPolicy.ANY;
    }

    @Override
    public boolean supportsHistory() {
        return false;
    }

    @Override
    public boolean supportsOfflinePlayers() {
        return true;
    }

    @Override
    public boolean supportsIdempotency() {
        return true;
    }

    @Override
    public synchronized long balance(@NotNull PointDefinition definition, @NotNull UUID player) {
        return balances.getOrDefault(player, definition.defaultUnits());
    }

    @Override
    public synchronized @NotNull StorageResult apply(@NotNull PointDefinition definition, @NotNull Mutation mutation) {
        if (mutation.idempotencyKey() != null && appliedKeys.containsKey(mutation.idempotencyKey())) {
            List<LedgerEntry> original = appliedKeys.get(mutation.idempotencyKey());
            return new StorageResult(Status.DUPLICATE, original.get(0).balance(), original);
        }
        StorageResult result = switch (mutation.operation()) {
            case ADD -> add(definition, mutation);
            case SUBTRACT -> subtract(definition, mutation);
            case SET -> set(definition, mutation);
            case TRANSFER -> transfer(definition, mutation);
        };
        if (mutation.idempotencyKey() != null && result.status() == Status.SUCCESS)
            appliedKeys.put(mutation.idempotencyKey(), result.entries());
        return result;
    }

    private StorageResult add(PointDefinition definition, Mutation mutation) {
        long current = balance(definition, mutation.player());
        long updated;
        try {
            updated = Math.addExact(current, mutation.units());
        } catch (ArithmeticException e) {
            return StorageResult.failure(Status.FAILED, current);
        }
        balances.put(mutation.player(), updated);
        return StorageResult.success(updated, List.of(
                LedgerEntry.unrecorded(definition.key(), mutation.player(), TransactionType.ADD, mutation.units(), updated, null, mutation)));
    }

    private StorageResult subtract(PointDefinition definition, Mutation mutation) {
        long current = balance(definition, mutation.player());
        if (current < mutation.units())
            return StorageResult.failure(Status.INSUFFICIENT_FUNDS, current);
        long updated = current - mutation.units();
        balances.put(mutation.player(), updated);
        return StorageResult.success(updated, List.of(
                LedgerEntry.unrecorded(definition.key(), mutation.player(), TransactionType.SUBTRACT, -mutation.units(), updated, null, mutation)));
    }

    private StorageResult set(PointDefinition definition, Mutation mutation) {
        long current = balance(definition, mutation.player());
        long delta;
        try {
            delta = Math.subtractExact(mutation.units(), current);
        } catch (ArithmeticException e) {
            return StorageResult.failure(Status.FAILED, current);
        }
        balances.put(mutation.player(), mutation.units());
        return StorageResult.success(mutation.units(), List.of(
                LedgerEntry.unrecorded(definition.key(), mutation.player(), TransactionType.SET, delta, mutation.units(), null, mutation)));
    }

    private StorageResult transfer(PointDefinition definition, Mutation mutation) {
        UUID receiver = mutation.receiver();
        long sender = balance(definition, mutation.player());
        if (sender < mutation.units())
            return StorageResult.failure(Status.INSUFFICIENT_FUNDS, sender);
        long credited;
        try {
            credited = Math.addExact(balance(definition, receiver), mutation.units());
        } catch (ArithmeticException e) {
            return StorageResult.failure(Status.FAILED, sender);
        }
        long debited = sender - mutation.units();
        balances.put(mutation.player(), debited);
        balances.put(receiver, credited);
        UUID correlation = UUID.randomUUID();
        return StorageResult.success(debited, List.of(
                LedgerEntry.unrecorded(definition.key(), mutation.player(), TransactionType.TRANSFER_OUT, -mutation.units(), debited, correlation, mutation),
                LedgerEntry.unrecorded(definition.key(), receiver, TransactionType.TRANSFER_IN, mutation.units(), credited, correlation, mutation.withoutIdempotencyKey())));
    }

}
