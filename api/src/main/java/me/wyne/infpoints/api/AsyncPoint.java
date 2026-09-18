package me.wyne.infpoints.api;

import me.wyne.infpoints.api.transaction.Transaction;
import me.wyne.infpoints.api.transaction.TransactionRequest;
import me.wyne.infpoints.api.transaction.TransactionResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous view of a {@link Point}, obtained through {@link Point#async()}.
 * <p>
 * Operations of one player run in the order they were submitted. Futures of database-backed points complete
 * on an InfPoints worker thread, futures of points bound to vanilla player data complete on the server's
 * main thread; switch to the main thread yourself before touching the world from a callback. Futures never
 * complete exceptionally for expected failures, those are reported the same way the synchronous methods
 * report them.
 */
public interface AsyncPoint {

    /**
     * Reads the player's balance from storage, bypassing the cache.
     *
     * @see Point#get(UUID)
     */
    @NotNull CompletableFuture<Double> get(@NotNull UUID player);

    /**
     * @see Point#add(UUID, double)
     */
    @NotNull CompletableFuture<Boolean> add(@NotNull UUID player, double amount);

    /**
     * @see Point#subtract(UUID, double)
     */
    @NotNull CompletableFuture<Boolean> subtract(@NotNull UUID player, double amount);

    /**
     * @see Point#set(UUID, double)
     */
    @NotNull CompletableFuture<Boolean> set(@NotNull UUID player, double amount);

    /**
     * @see Point#transfer(UUID, UUID, double)
     */
    @NotNull CompletableFuture<Boolean> transfer(@NotNull UUID sender, @NotNull UUID receiver, double amount);

    /**
     * @see Point#execute(TransactionRequest)
     */
    @NotNull CompletableFuture<TransactionResult> execute(@NotNull TransactionRequest request);

    /**
     * Returns the player's transactions, newest first. Completes with an empty list when the point
     * {@linkplain Point#supportsHistory() doesn't keep history} or the history can't be read.
     *
     * @param offset number of newest transactions to skip
     * @param limit  maximum number of transactions to return
     */
    @NotNull CompletableFuture<List<Transaction>> history(@NotNull UUID player, int offset, int limit);

    /**
     * Returns the transaction of this point with the given id, if it exists and can be read.
     */
    @NotNull CompletableFuture<Optional<Transaction>> transaction(long id);

}
