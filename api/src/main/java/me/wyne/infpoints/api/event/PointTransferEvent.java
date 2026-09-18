package me.wyne.infpoints.api.event;

import me.wyne.infpoints.api.Point;
import me.wyne.infpoints.api.transaction.TransactionRequest;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Fired before an amount is moved from {@link #getPlayer()} to {@link #getReceiver()}. A changed amount
 * applies to both sides of the transfer.
 */
public class PointTransferEvent extends PointEvent {

    public PointTransferEvent(@NotNull Point point, @NotNull TransactionRequest request) {
        super(point, request);
    }

    /**
     * Returns the player receiving the amount.
     */
    public @NotNull UUID getReceiver() {
        return Objects.requireNonNull(getRequest().receiver());
    }

}
