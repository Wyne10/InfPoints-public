package me.wyne.infpoints.api.event;

import me.wyne.infpoints.api.Point;
import me.wyne.infpoints.api.transaction.TransactionRequest;
import org.jetbrains.annotations.NotNull;

/**
 * Fired before an amount is subtracted from a point's balance. The subtraction may still fail afterwards
 * when the balance is insufficient.
 */
public class PointSubtractEvent extends PointEvent {
    public PointSubtractEvent(@NotNull Point point, @NotNull TransactionRequest request) {
        super(point, request);
    }
}
