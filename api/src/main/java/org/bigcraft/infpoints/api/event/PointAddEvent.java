package org.bigcraft.infpoints.api.event;

import org.bigcraft.infpoints.api.Point;
import org.bigcraft.infpoints.api.transaction.TransactionRequest;
import org.jetbrains.annotations.NotNull;

/**
 * Fired before an amount is added to a point's balance.
 */
public class PointAddEvent extends PointEvent {
    public PointAddEvent(@NotNull Point point, @NotNull TransactionRequest request) {
        super(point, request);
    }
}
