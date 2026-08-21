package org.bigcraft.infpoints.api.event;

import org.bigcraft.infpoints.api.Point;
import org.bigcraft.infpoints.api.PointConfig;
import org.bigcraft.infpoints.api.PointType;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Bukkit event fired when a {@link Point}'s balance is about to change.
 * <p>
 * The kind of change is reported by {@link #getType()}; concrete subclasses
 * {@link PointAddEvent}, {@link PointSubtractEvent} and {@link PointSetEvent}
 * exist so listeners can also filter by event class. The event is cancellable;
 * if cancelled, the balance is left unchanged. Handlers may call
 * {@link #setAmount(double)} to change how much is applied before the event
 * completes. The event is raised asynchronously when it is fired off the
 * server's primary thread.
 */
public abstract class PointEvent extends Event implements Cancellable {

    private final static HandlerList HANDLER_LIST = new HandlerList();

    private final Point point;
    private final UUID player;
    private double amount;
    private final PointEventType type;
    private boolean cancelled;

    /**
     * @param point  the point whose balance is changing
     * @param player the affected player
     * @param amount the amount being applied; see {@link #getType()} for how it is used
     * @param type   the kind of change this event represents
     */
    public PointEvent(Point point, UUID player, double amount, PointEventType type) {
        super(!Bukkit.isPrimaryThread());
        this.point = point;
        this.player = player;
        this.amount = amount;
        this.type = type;
    }

    /**
     * Changes the amount that will be applied when this event is not cancelled.
     * Also changes the result of {@link #getNewBalance()}.
     */
    public void setAmount(double amount) {
        this.amount = amount;
    }

    /**
     * Returns the point whose balance is changing.
     */
    public Point getPoint() {
        return point;
    }

    /**
     * Returns the {@link PointType} view of the point whose balance is changing.
     */
    public PointType getPointType() {
        return point;
    }

    /**
     * Returns the {@link PointConfig} view of the point whose balance is changing.
     */
    public PointConfig getPointConfig() {
        return point;
    }

    /**
     * Returns the UUID of the affected player.
     */
    public UUID getPlayer() {
        return player;
    }

    /**
     * Returns the amount being applied, as passed to the constructor or last
     * changed via {@link #setAmount(double)}.
     */
    public double getAmount() {
        return amount;
    }

    /**
     * Returns the player's balance before this event is applied.
     */
    public double getBalance() {
        return point.get(player);
    }

    /**
     * Returns what the player's balance would become if this event completes
     * unmodified: the current balance plus {@link #getAmount()} for
     * {@link PointEventType#ADD}, minus it for {@link PointEventType#SUBTRACT},
     * or {@link #getAmount()} itself for {@link PointEventType#SET}.
     */
    public double getNewBalance() {
        if (type == PointEventType.ADD)
            return point.get(player) + amount;
        else if (type == PointEventType.SUBTRACT)
            return point.get(player) - amount;
        else
            return amount;
    }

    /**
     * Returns the kind of change this event represents.
     */
    public PointEventType getType() {
        return type;
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
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

}
