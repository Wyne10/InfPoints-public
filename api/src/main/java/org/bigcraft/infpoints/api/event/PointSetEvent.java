package org.bigcraft.infpoints.api.event;

import org.bigcraft.infpoints.api.Point;
import org.bigcraft.infpoints.api.transaction.TransactionRequest;
import org.jetbrains.annotations.NotNull;

/**
 * Fired before a point's balance is replaced; {@link #getAmount()} is the new balance.
 */
public class PointSetEvent extends PointEvent {
    public PointSetEvent(@NotNull Point point, @NotNull TransactionRequest request) {
        super(point, request);
    }
}
