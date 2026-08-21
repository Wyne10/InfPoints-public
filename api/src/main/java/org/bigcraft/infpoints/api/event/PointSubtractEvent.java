package org.bigcraft.infpoints.api.event;

import org.bigcraft.infpoints.api.Point;

import java.util.UUID;

/**
 * Fired when a {@link Point}'s balance is about to decrease; {@link #getType()}
 * is always {@link PointEventType#SUBTRACT}.
 */
public class PointSubtractEvent extends PointEvent {
    public PointSubtractEvent(Point point, UUID player, double amount, PointEventType type) {
        super(point, player, amount, type);
    }
}
