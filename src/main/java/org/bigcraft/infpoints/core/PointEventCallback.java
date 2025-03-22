package org.bigcraft.infpoints.core;

import org.bigcraft.infpoints.api.event.PointEventType;

import java.util.UUID;

public final class PointEventCallback extends Point {

    private final Point parent;

    public PointEventCallback(Point parent) {
        super(parent);
        this.parent = parent;
    }

    @Override
    public long get(UUID player) {
        return parent.get(player);
    }

    @Override
    public void add(UUID player, long amount) {
        var event = PointEventType.ADD.call(this, this, player, amount);
        if (!event.callEvent())
            return;
        parent.add(player, event.getAmount());
    }

    @Override
    public boolean subtract(UUID player, long amount) {
        var event = PointEventType.SUBTRACT.call(this, this, player, amount);
        if (!event.callEvent())
            return false;
        return parent.subtract(player, event.getAmount());
    }

    @Override
    public void set(UUID player, long amount) {
        var event = PointEventType.SET.call(this, this, player, amount);
        if (!event.callEvent())
            return;
        parent.set(player, event.getAmount());
    }

    @Override
    public boolean transfer(UUID sender, UUID receiver, long amount) {
        if (!subtract(sender, amount))
            return false;
        add(receiver, amount);
        return true;
    }

}
