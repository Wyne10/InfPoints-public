package org.bigcraft.infpoints.api.event;

import org.bigcraft.infpoints.api.Point;
import org.bigcraft.infpoints.api.transaction.TransactionRequest;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired before a point's balance changes.
 * <p>
 * The concrete subclasses {@link PointAddEvent}, {@link PointSubtractEvent}, {@link PointSetEvent} and
 * {@link PointTransferEvent} let listeners filter by operation. Cancelling the event leaves the balance
 * unchanged; {@link #setAmount(double)} changes the amount that is applied. The event is asynchronous when
 * the operation runs off the server's main thread, so check {@link #isAsynchronous()} before touching the
 * world. Listen to {@link PointTransactionCompleteEvent} to react to changes that were actually applied.
 */
public abstract class PointEvent extends Event implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Point point;
    private TransactionRequest request;
    private boolean cancelled;

    protected PointEvent(@NotNull Point point, @NotNull TransactionRequest request) {
        super(!Bukkit.isPrimaryThread());
        this.point = point;
        this.request = request;
    }

    /**
     * Returns the point whose balance is about to change.
     */
    public @NotNull Point getPoint() {
        return point;
    }

    /**
     * Returns the request that will be applied, including amount changes made by earlier handlers.
     */
    public @NotNull TransactionRequest getRequest() {
        return request;
    }

    /**
     * Returns the affected player, the sender for transfers.
     */
    public @NotNull UUID getPlayer() {
        return request.player();
    }

    /**
     * Returns the amount that will be applied.
     */
    public double getAmount() {
        return request.amount();
    }

    /**
     * Changes the amount that will be applied when this event isn't cancelled.
     */
    public void setAmount(double amount) {
        request = request.withAmount(amount);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }

    /**
     * Returns the static handler list Bukkit uses to register listeners for this event type.
     */
    public static @NotNull HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

}
