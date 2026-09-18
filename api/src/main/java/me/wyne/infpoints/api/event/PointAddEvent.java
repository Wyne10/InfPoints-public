package me.wyne.infpoints.api.event;

import me.wyne.infpoints.api.Point;
import me.wyne.infpoints.api.transaction.TransactionRequest;
import org.jetbrains.annotations.NotNull;

/**
 * Fired before an amount is added to a point's balance.
 */
public class PointAddEvent extends PointEvent {
    public PointAddEvent(@NotNull Point point, @NotNull TransactionRequest request) {
        super(point, request);
    }
}
