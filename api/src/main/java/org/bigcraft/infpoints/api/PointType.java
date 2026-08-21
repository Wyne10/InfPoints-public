package org.bigcraft.infpoints.api;

import java.util.UUID;

/**
 * Core balance operations for a single point type, keyed by player {@link UUID}.
 * <p>
 * This is the storage-facing counterpart to {@link PointView}: it performs no
 * player messaging and only manipulates the underlying balance. Implementations
 * returned by the plugin typically fire a corresponding cancellable event from
 * {@code org.bigcraft.infpoints.api.event} before applying a change.
 */
public interface PointType {

    /**
     * Returns the player's current balance.
     */
    double get(UUID player);

    /**
     * Adds {@code amount} to the player's balance.
     */
    void add(UUID player, double amount);

    /**
     * Subtracts {@code amount} from the player's balance, if the balance is sufficient.
     *
     * @return {@code true} if the balance was sufficient and the subtraction was applied,
     *         {@code false} if the balance was left unchanged
     */
    boolean subtract(UUID player, double amount);

    /**
     * Sets the player's balance to {@code amount}, replacing any previous value.
     */
    void set(UUID player, double amount);

    /**
     * Moves {@code amount} from the sender's balance to the receiver's balance.
     * <p>
     * Equivalent to a {@link #subtract} on the sender followed by an {@link #add}
     * on the receiver; if the sender's balance is insufficient, neither balance changes.
     *
     * @return {@code true} if the transfer was applied, {@code false} if the sender
     *         had an insufficient balance
     */
    boolean transfer(UUID sender, UUID receiver, double amount);

}
