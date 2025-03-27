package org.bigcraft.infpoints.api.event;

import org.bigcraft.infpoints.api.PointConfig;
import org.bigcraft.infpoints.api.PointType;

import java.util.UUID;

public enum PointEventType {
    ADD(PointAddEvent::new),
    SUBTRACT(PointSubtractEvent::new),
    SET(PointSetEvent::new),;

    @FunctionalInterface
    interface PointEventFactory {
        PointEvent create(PointType pointType, PointConfig pointConfig, UUID player, double amount, PointEventType type);
    }
    private final PointEventFactory factory;

    PointEventType(PointEventFactory factory) {
        this.factory = factory;
    }

    public PointEvent call(PointType pointType, PointConfig pointConfig, UUID player, double amount) {
        return this.factory.create(pointType, pointConfig, player, amount, this);
    }
}
