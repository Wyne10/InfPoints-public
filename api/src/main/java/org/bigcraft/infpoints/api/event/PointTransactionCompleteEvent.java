package org.bigcraft.infpoints.api.event;

import org.bigcraft.infpoints.api.Point;
import org.bigcraft.infpoints.api.transaction.TransactionRequest;
import org.bigcraft.infpoints.api.transaction.TransactionResult;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired on the server's main thread after a balance change was applied and stored.
 */
public class PointTransactionCompleteEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Point point;
    private final TransactionRequest request;
    private final TransactionResult result;

    public PointTransactionCompleteEvent(@NotNull Point point, @NotNull TransactionRequest request, @NotNull TransactionResult result) {
        this.point = point;
        this.request = request;
        this.result = result;
    }

    /**
     * Returns the point whose balance changed.
     */
    public @NotNull Point getPoint() {
        return point;
    }

    /**
     * Returns the request as it was applied, after event handlers changed it.
     */
    public @NotNull TransactionRequest getRequest() {
        return request;
    }

    /**
     * Returns the outcome, including the recorded transactions and the new balance.
     */
    public @NotNull TransactionResult getResult() {
        return result;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

}
