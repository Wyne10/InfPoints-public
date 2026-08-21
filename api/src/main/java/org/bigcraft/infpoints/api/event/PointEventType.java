package org.bigcraft.infpoints.api.event;

import org.bigcraft.infpoints.api.Point;

import java.util.UUID;

/**
 * Kind of balance change a {@link PointEvent} represents, paired with the
 * concrete {@link PointEvent} subclass fired for it.
 */
public enum PointEventType {
    /** Balance is increasing; fires a {@link PointAddEvent}. */
    ADD(PointAddEvent::new),
    /** Balance is decreasing; fires a {@link PointSubtractEvent}. */
    SUBTRACT(PointSubtractEvent::new),
    /** Balance is being replaced; fires a {@link PointSetEvent}. */
    SET(PointSetEvent::new),;

    // Constructs the PointEvent subclass associated with a given enum constant.
    @FunctionalInterface
    interface PointEventFactory {
        PointEvent create(Point point, UUID player, double amount, PointEventType type);
    }
    private final PointEventFactory factory;

    PointEventType(PointEventFactory factory) {
        this.factory = factory;
    }

    /**
     * Creates the {@link PointEvent} subclass corresponding to this type, for the
     * given point, player and amount. Does not fire the event.
     */
    public PointEvent create(Point point, UUID player, double amount) {
        return this.factory.create(point, player, amount, this);
    }
}
