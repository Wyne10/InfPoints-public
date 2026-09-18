package org.bigcraft.infpoints.storage;

import org.bigcraft.infpoints.api.transaction.TransactionResult.Status;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record StorageResult(@NotNull Status status, long balance, @NotNull List<LedgerEntry> entries) {

    public static final long UNKNOWN_BALANCE = Long.MIN_VALUE;

    public StorageResult {
        entries = List.copyOf(entries);
    }

    public static @NotNull StorageResult success(long balance, @NotNull List<LedgerEntry> entries) {
        return new StorageResult(Status.SUCCESS, balance, entries);
    }

    public static @NotNull StorageResult failure(@NotNull Status status, long balance) {
        return new StorageResult(status, balance, List.of());
    }

    public boolean hasBalance() {
        return balance != UNKNOWN_BALANCE;
    }

}
