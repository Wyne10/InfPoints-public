package org.bigcraft.infpoints.api;

import org.bukkit.entity.Player;

/**
 * Player-facing balance operations for a single point type.
 * <p>
 * Mirrors {@link PointType}, but accepts a {@link Player} instead of a raw
 * player UUID so implementations can also notify the player about the
 * outcome of the operation.
 */
public interface PointView {

    /**
     * Adds {@code amount} to the player's balance.
     */
    void add(Player player, double amount);

    /**
     * Subtracts {@code amount} from the player's balance, if the balance is sufficient.
     *
     * @return {@code true} if the balance was sufficient and the subtraction was applied,
     *         {@code false} if the balance was left unchanged
     */
    boolean subtract(Player player, double amount);

    /**
     * Sets the player's balance to {@code amount}, replacing any previous value.
     */
    void set(Player player, double amount);

    /**
     * Moves {@code amount} from the sender's balance to the receiver's balance.
     * <p>
     * Equivalent to a {@link #subtract} on the sender followed by an {@link #add}
     * on the receiver; if the sender's balance is insufficient, neither balance changes.
     *
     * @return {@code true} if the transfer was applied, {@code false} if the sender
     *         had an insufficient balance
     */
    boolean transfer(Player sender, Player receiver, double amount);

}
