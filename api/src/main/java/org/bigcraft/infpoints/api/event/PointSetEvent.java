package org.bigcraft.infpoints.api.event;

import org.bigcraft.infpoints.api.PointConfig;
import org.bigcraft.infpoints.api.PointType;

import java.util.UUID;

public class PointSetEvent extends PointEvent {
    public PointSetEvent(PointType pointType, PointConfig pointConfig, UUID player, long amount, PointEventType type) {
        super(pointType, pointConfig, player, amount, type);
    }
}
