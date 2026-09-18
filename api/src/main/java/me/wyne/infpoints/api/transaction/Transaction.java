package me.wyne.infpoints.api.transaction;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * A recorded balance change of one player.
 *
 * @param id             identifier within the transaction history, {@code 0} for points without history
 * @param point          key of the point
 * @param player         the player whose balance changed
 * @param type           the kind of change
 * @param amount         the signed change of the balance
 * @param balance        the balance right after the change
 * @param correlationId  shared by both sides of a transfer, {@code null} otherwise
 * @param idempotencyKey the idempotency key of the request, stored on the sender side of a transfer
 * @param source         what initiated the change, e.g. a plugin name or {@code command:add}
 * @param actor          the player who initiated the change
 * @param reason         free-form description of the change
 * @param server         name of the server that applied the change
 * @param createdAt      when the change was applied
 */
public record Transaction(long id, @NotNull String point, @NotNull UUID player, @NotNull TransactionType type,
                          double amount, double balance, @Nullable UUID correlationId, @Nullable String idempotencyKey,
                          @Nullable String source, @Nullable UUID actor, @Nullable String reason, @Nullable String server,
                          @NotNull Instant createdAt) {
}
