package org.bigcraft.infpoints.core;

import org.bigcraft.infpoints.api.event.PointEventType;

import java.util.UUID;

public final class PointEventCallback extends Point implements AutoCloseable {

    private final Point parent;

    public PointEventCallback(Point parent) {
        super(parent);
        this.parent = parent;
    }

    @Override
    public double get(UUID player) {
        return parent.get(player);
    }

    @Override
    public void add(UUID player, double amount) {
        var event = PointEventType.ADD.create(this, player, amount);
        if (!event.callEvent())
            return;
        parent.add(player, event.getAmount());
    }

    @Override
    public boolean subtract(UUID player, double amount) {
        var event = PointEventType.SUBTRACT.create(this, player, amount);
        if (!event.callEvent())
            return false;
        return parent.subtract(player, event.getAmount());
    }

    @Override
    public void set(UUID player, double amount) {
        var event = PointEventType.SET.create(this, player, amount);
        if (!event.callEvent())
            return;
        parent.set(player, event.getAmount());
    }

    @Override
    public void close() throws Exception {
        if (parent instanceof AutoCloseable c) c.close();
    }
}
