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

public abstract class PointEvent extends Event implements Cancellable {

    private final static HandlerList HANDLER_LIST = new HandlerList();

    private final Point point;
    private final UUID player;
    private double amount;
    private final PointEventType type;
    private boolean cancelled;

    public PointEvent(Point point, UUID player, double amount, PointEventType type) {
        super(!Bukkit.isPrimaryThread());
        this.point = point;
        this.player = player;
        this.amount = amount;
        this.type = type;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Point getPoint() {
        return point;
    }

    public PointType getPointType() {
        return point;
    }

    public PointConfig getPointConfig() {
        return point;
    }

    public UUID getPlayer() {
        return player;
    }

    public double getAmount() {
        return amount;
    }

    public double getBalance() {
        return point.get(player);
    }

    public double getNewBalance() {
        if (type == PointEventType.ADD)
            return point.get(player) + amount;
        else if (type == PointEventType.SUBTRACT)
            return point.get(player) - amount;
        else
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
