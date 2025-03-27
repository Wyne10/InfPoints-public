package org.bigcraft.infpoints.api.event;

import org.bigcraft.infpoints.api.PointConfig;
import org.bigcraft.infpoints.api.PointType;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public abstract class PointEvent extends Event implements Cancellable {

    private final static HandlerList HANDLER_LIST = new HandlerList();

    private final PointType pointType;
    private final PointConfig pointConfig;
    private final UUID player;
    private double amount;
    private final PointEventType type;
    private boolean cancelled;

    public PointEvent(PointType pointType, PointConfig pointConfig, UUID player, double amount, PointEventType type) {
        super();
        this.pointType = pointType;
        this.pointConfig = pointConfig;
        this.player = player;
        this.amount = amount;
        this.type = type;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public PointType getPointType() {
        return pointType;
    }

    public PointConfig getPointConfig() {
        return pointConfig;
    }

    public UUID getPlayer() {
        return player;
    }

    public double getAmount() {
        return amount;
    }

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

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

}
