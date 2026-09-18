package me.wyne.infpoints.storage;

import me.wyne.infpoints.api.transaction.TransactionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public record LedgerEntry(long id, @NotNull String point, @NotNull UUID player, @NotNull TransactionType type, long units,
                          long balance, @Nullable UUID correlationId, @Nullable String idempotencyKey, @Nullable String source,
                          @Nullable UUID actor, @Nullable String reason, @Nullable String server, @NotNull Instant createdAt) {

    public static @NotNull LedgerEntry unrecorded(@NotNull String point, @NotNull UUID player, @NotNull TransactionType type, long units,
                                                  long balance, @Nullable UUID correlationId, @NotNull Mutation mutation) {
        return new LedgerEntry(0, point, player, type, units, balance, correlationId, mutation.idempotencyKey(),
                mutation.source(), mutation.actor(), mutation.reason(), null, Instant.now());
    }

}
