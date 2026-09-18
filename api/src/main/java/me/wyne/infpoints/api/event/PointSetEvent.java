package me.wyne.infpoints.api.event;

import me.wyne.infpoints.api.Point;
import me.wyne.infpoints.api.transaction.TransactionRequest;
import org.jetbrains.annotations.NotNull;

/**
 * Fired before a point's balance is replaced; {@link #getAmount()} is the new balance.
 */
public class PointSetEvent extends PointEvent {
    public PointSetEvent(@NotNull Point point, @NotNull TransactionRequest request) {
        super(point, request);
    }
}
