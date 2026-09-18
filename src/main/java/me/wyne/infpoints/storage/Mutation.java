package me.wyne.infpoints.storage;

import me.wyne.infpoints.api.transaction.TransactionRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record Mutation(@NotNull TransactionRequest.Operation operation, @NotNull UUID player, @Nullable UUID receiver, long units,
                       @Nullable String reason, @Nullable String source, @Nullable UUID actor, @Nullable String idempotencyKey) {

    public static @NotNull Mutation of(@NotNull TransactionRequest request, long units) {
        return new Mutation(request.operation(), request.player(), request.receiver(), units,
                request.reason(), request.source(), request.actor(), request.idempotencyKey());
    }

    // The receiving side of a transfer is recorded without the key, which belongs to the sender side
    public @NotNull Mutation withoutIdempotencyKey() {
        return new Mutation(operation, player, receiver, units, reason, source, actor, null);
    }

}
