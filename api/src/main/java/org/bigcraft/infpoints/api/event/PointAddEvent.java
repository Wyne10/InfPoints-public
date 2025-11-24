package org.bigcraft.infpoints.api.event;

import org.bigcraft.infpoints.api.Point;

import java.util.UUID;

public class PointAddEvent extends PointEvent {
    public PointAddEvent(Point point, UUID player, double amount, PointEventType type) {
        super(point, player, amount, type);
    }
}
