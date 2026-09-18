package org.bigcraft.infpoints.api;

import org.bigcraft.infpoints.api.config.CommandConfig;
import org.bigcraft.infpoints.api.config.PointConfig;
import org.bigcraft.infpoints.api.config.VisualConfig;
import org.bigcraft.infpoints.api.transaction.TransactionRequest;
import org.bigcraft.infpoints.api.transaction.TransactionResult;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalDouble;
import java.util.UUID;

/**
 * A single configured point (currency).
 * <p>
 * Instances are looked up by key through {@link PointProvider#getPoint(String)} and stay valid for the
 * lifetime of the plugin: a reload updates the same object, and a point removed from the configuration
 * becomes {@linkplain #isAvailable() unavailable} instead of breaking references held by other plugins.
 * <p>
 * Balances are exact to {@link PointConfig#decimals()} decimal places; amounts are rounded half-up to that
 * precision before they are applied. Synchronous methods may block while a database-backed point talks to
 * its database, so prefer {@link #async()} for frequent or bulk operations.
 */
public interface Point {

    /**
     * Returns the configuration key identifying this point.
     */
    @NotNull String getKey();

    /**
     * Returns the storage configuration (key, storage type, default balance and decimals) of this point.
     */
    @NotNull PointConfig getConfig();

    /**
     * Returns the display configuration (name, symbol, color and number format) of this point.
     */
    @NotNull VisualConfig getVisualConfig();

    /**
     * Returns the pay/balance command configuration of this point.
     */
    @NotNull CommandConfig getCommandConfig();

    /**
     * Returns whether operations can currently be applied: the point is still configured and its storage is
     * usable. Operations on an unavailable point fail with {@link TransactionResult.Status#UNAVAILABLE}.
     */
    boolean isAvailable();

    /**
     * Returns whether this point keeps a transaction history, see {@link AsyncPoint#history}.
     */
    boolean supportsHistory();

    /**
     * Returns whether balances of offline players can be read and changed. Operations on offline players of a
     * point that doesn't support them fail with {@link TransactionResult.Status#PLAYER_OFFLINE}.
     */
    boolean supportsOfflinePlayers();

    /**
     * Returns the player's balance.
     * <p>
     * Database-backed points may answer from a short-lived cache kept up to date by changes made on this
     * server; changes made by other servers sharing the database become visible within the configured
     * refresh interval. Returns the last known balance, or {@code 0} if there is none, when the balance
     * can't be read.
     */
    double get(@NotNull UUID player);

    /**
     * Returns the last known balance without ever blocking, loading it in the background when it is missing
     * or outdated. Safe to call from any thread, e.g. from placeholders.
     *
     * @return the last known balance, or an empty value if it isn't known yet
     */
    @NotNull OptionalDouble getCached(@NotNull UUID player);

    /**
     * Formats the player's {@linkplain #get(UUID) balance} with {@link VisualConfig#decimalFormat()}.
     */
    @NotNull String getFormat(@NotNull UUID player);

    /**
     * Adds a positive {@code amount} to the player's balance.
     *
     * @return {@code true} if the amount was added
     */
    boolean add(@NotNull UUID player, double amount);

    /**
     * Subtracts a positive {@code amount} from the player's balance if the balance is sufficient.
     *
     * @return {@code true} if the amount was subtracted, {@code false} if the balance was insufficient or the
     *         operation failed, in which case the balance is unchanged
     */
    boolean subtract(@NotNull UUID player, double amount);

    /**
     * Replaces the player's balance with a non-negative {@code amount}.
     *
     * @return {@code true} if the balance was set
     */
    boolean set(@NotNull UUID player, double amount);

    /**
     * Atomically moves a positive {@code amount} from the sender's balance to the receiver's balance.
     * Either both balances change or neither does.
     *
     * @return {@code true} if the transfer was applied, {@code false} if the sender's balance was insufficient
     *         or the operation failed
     */
    boolean transfer(@NotNull UUID sender, @NotNull UUID receiver, double amount);

    /**
     * Applies {@code request}, which may carry a reason, source, actor and idempotency key that are stored in
     * the transaction history.
     *
     * @return the outcome, never {@code null}; expected failures are reported by
     *         {@link TransactionResult#status()} rather than thrown
     */
    @NotNull TransactionResult execute(@NotNull TransactionRequest request);

    /**
     * Same as {@link #add(UUID, double)}, additionally notifying the player once the amount was added.
     */
    boolean add(@NotNull Player player, double amount);

    /**
     * Same as {@link #subtract(UUID, double)}, additionally notifying the player about the outcome.
     */
    boolean subtract(@NotNull Player player, double amount);

    /**
     * Same as {@link #set(UUID, double)}, additionally notifying the player once the balance was set.
     */
    boolean set(@NotNull Player player, double amount);

    /**
     * Same as {@link #transfer(UUID, UUID, double)}, additionally notifying both players about the outcome.
     */
    boolean transfer(@NotNull Player sender, @NotNull Player receiver, double amount);

    /**
     * Returns the asynchronous view of this point, mirroring the synchronous operations with
     * {@link java.util.concurrent.CompletableFuture} results.
     */
    @NotNull AsyncPoint async();

}
