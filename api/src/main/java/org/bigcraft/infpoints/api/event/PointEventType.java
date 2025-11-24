package org.bigcraft.infpoints.api.event;

import org.bigcraft.infpoints.api.Point;

import java.util.UUID;

public enum PointEventType {
    ADD(PointAddEvent::new),
    SUBTRACT(PointSubtractEvent::new),
    SET(PointSetEvent::new),;

    @FunctionalInterface
    interface PointEventFactory {
        PointEvent create(Point point, UUID player, double amount, PointEventType type);
    }
    private final PointEventFactory factory;

    PointEventType(PointEventFactory factory) {
        this.factory = factory;
    }

    public PointEvent create(Point point, UUID player, double amount) {
        return this.factory.create(point, player, amount, this);
    }
}
