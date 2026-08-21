package org.bigcraft.infpoints.api.event;

import org.bigcraft.infpoints.api.Point;

import java.util.UUID;

/**
 * Fired when a {@link Point}'s balance is about to be replaced; {@link #getType()}
 * is always {@link PointEventType#SET}.
 */
public class PointSetEvent extends PointEvent {
    public PointSetEvent(Point point, UUID player, double amount, PointEventType type) {
        super(point, player, amount, type);
    }
}
