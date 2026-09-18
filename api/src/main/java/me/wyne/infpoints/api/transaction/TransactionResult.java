package me.wyne.infpoints.api.transaction;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Outcome of a {@link TransactionRequest}.
 *
 * @param status       what happened
 * @param amount       the amount after rounding and after event handlers changed it
 * @param balance      the player's balance after the operation, the current balance for
 *                     {@link Status#INSUFFICIENT_FUNDS}, or {@link Double#NaN} when it isn't known
 * @param transactions the recorded transactions: one per affected player for applied operations, the
 *                     original ones for {@link Status#DUPLICATE}, empty otherwise
 */
public record TransactionResult(@NotNull Status status, double amount, double balance, @NotNull List<Transaction> transactions) {

    public enum Status {
        /** The change was applied. */
        SUCCESS,
        /** A request with the same idempotency key was already applied; nothing changed this time. */
        DUPLICATE,
        /** The balance was too low, nothing changed. */
        INSUFFICIENT_FUNDS,
        /** An event handler cancelled the change. */
        CANCELLED,
        /** The amount was not a finite number, not positive, negative for a set, or rounded to zero. */
        INVALID_AMOUNT,
        /** The point only supports online players and a player involved is offline. */
        PLAYER_OFFLINE,
        /** The point is no longer configured or its storage can't be reached, nothing changed. */
        UNAVAILABLE,
        /** The storage reported an error; the change was rolled back, the server log has details. */
        FAILED
    }

    public TransactionResult {
        Objects.requireNonNull(status, "status");
        transactions = List.copyOf(transactions);
    }

    /**
     * Returns whether the requested change is in effect: it was applied now, or earlier by a request with the
     * same idempotency key.
     */
    public boolean isApplied() {
        return status == Status.SUCCESS || status == Status.DUPLICATE;
    }

}
