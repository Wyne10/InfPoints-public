package me.wyne.infpoints.api.transaction;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * A balance change to apply to a point through {@link me.wyne.infpoints.api.Point#execute}.
 * <p>
 * Create one with {@link #add}, {@link #subtract}, {@link #set} or {@link #transfer} and attach metadata
 * with the {@code with…} methods, e.g.
 * {@code TransactionRequest.add(player, 100).withReason("Store order").withIdempotencyKey("order-1234")}.
 *
 * @param operation      the kind of change
 * @param player         the affected player, the sender for transfers
 * @param receiver       the receiver of a transfer, {@code null} for every other operation
 * @param amount         the amount to add, subtract, transfer, or the new balance for {@link Operation#SET}
 * @param reason         free-form description stored in the history, truncated to 255 characters
 * @param source         what initiated the change, stored in the history and truncated to 64 characters;
 *                       defaults to the name of the calling plugin
 * @param actor          the player who initiated the change, e.g. the sender of a command
 * @param idempotencyKey key making the request apply at most once; a repeated request with the same key
 *                       returns the original transaction with {@link TransactionResult.Status#DUPLICATE}
 * @param requireIdempotency whether the key must be honoured, see {@link #withIdempotencyKey} and
 *                           {@link #withBestEffortIdempotencyKey}
 */
public record TransactionRequest(@NotNull Operation operation, @NotNull UUID player, @Nullable UUID receiver,
                                 double amount, @Nullable String reason, @Nullable String source,
                                 @Nullable UUID actor, @Nullable String idempotencyKey, boolean requireIdempotency) {

    /**
     * Maximum length of an {@linkplain #idempotencyKey() idempotency key}.
     */
    public static final int MAX_IDEMPOTENCY_KEY_LENGTH = 128;

    /**
     * Kind of balance change a request describes.
     */
    public enum Operation {
        ADD,
        SUBTRACT,
        SET,
        TRANSFER
    }

    /**
     * @throws IllegalArgumentException if a transfer has no receiver or sends to itself, another operation has
     *                                  a receiver, or the idempotency key is empty or too long
     */
    public TransactionRequest {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(player, "player");
        if (operation == Operation.TRANSFER && receiver == null)
            throw new IllegalArgumentException("A transfer requires a receiver");
        if (operation == Operation.TRANSFER && receiver.equals(player))
            throw new IllegalArgumentException("A transfer requires different sender and receiver");
        if (operation != Operation.TRANSFER && receiver != null)
            throw new IllegalArgumentException("Only a transfer has a receiver");
        if (idempotencyKey != null && (idempotencyKey.isEmpty() || idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH))
            throw new IllegalArgumentException("Idempotency key must contain 1 to " + MAX_IDEMPOTENCY_KEY_LENGTH + " characters");
    }

    /**
     * Creates a request whose idempotency key, if any, is required.
     *
     * @throws IllegalArgumentException as the {@linkplain #TransactionRequest canonical constructor} does
     */
    public TransactionRequest(@NotNull Operation operation, @NotNull UUID player, @Nullable UUID receiver,
                              double amount, @Nullable String reason, @Nullable String source,
                              @Nullable UUID actor, @Nullable String idempotencyKey) {
        this(operation, player, receiver, amount, reason, source, actor, idempotencyKey, true);
    }

    public static @NotNull TransactionRequest add(@NotNull UUID player, double amount) {
        return new TransactionRequest(Operation.ADD, player, null, amount, null, null, null, null);
    }

    public static @NotNull TransactionRequest subtract(@NotNull UUID player, double amount) {
        return new TransactionRequest(Operation.SUBTRACT, player, null, amount, null, null, null, null);
    }

    public static @NotNull TransactionRequest set(@NotNull UUID player, double amount) {
        return new TransactionRequest(Operation.SET, player, null, amount, null, null, null, null);
    }

    public static @NotNull TransactionRequest transfer(@NotNull UUID sender, @NotNull UUID receiver, double amount) {
        return new TransactionRequest(Operation.TRANSFER, sender, receiver, amount, null, null, null, null);
    }

    public @NotNull TransactionRequest withAmount(double amount) {
        return new TransactionRequest(operation, player, receiver, amount, reason, source, actor, idempotencyKey, requireIdempotency);
    }

    public @NotNull TransactionRequest withReason(@Nullable String reason) {
        return new TransactionRequest(operation, player, receiver, amount, reason, source, actor, idempotencyKey, requireIdempotency);
    }

    public @NotNull TransactionRequest withSource(@Nullable String source) {
        return new TransactionRequest(operation, player, receiver, amount, reason, source, actor, idempotencyKey, requireIdempotency);
    }

    public @NotNull TransactionRequest withActor(@Nullable UUID actor) {
        return new TransactionRequest(operation, player, receiver, amount, reason, source, actor, idempotencyKey, requireIdempotency);
    }

    /**
     * Attaches a key the point must honour: a request carrying one fails with
     * {@link TransactionResult.Status#FAILED} when the point doesn't
     * {@linkplain me.wyne.infpoints.api.Point#supportsIdempotency() support idempotency}.
     */
    public @NotNull TransactionRequest withIdempotencyKey(@Nullable String idempotencyKey) {
        return new TransactionRequest(operation, player, receiver, amount, reason, source, actor, idempotencyKey, true);
    }

    /**
     * Attaches a key the point honours where it can: a request carrying one still applies, without the
     * at-most-once guarantee, when the point doesn't
     * {@linkplain me.wyne.infpoints.api.Point#supportsIdempotency() support idempotency}.
     */
    public @NotNull TransactionRequest withBestEffortIdempotencyKey(@Nullable String idempotencyKey) {
        return new TransactionRequest(operation, player, receiver, amount, reason, source, actor, idempotencyKey, false);
    }

}
