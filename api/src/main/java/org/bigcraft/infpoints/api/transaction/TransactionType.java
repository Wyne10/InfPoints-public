package org.bigcraft.infpoints.api.transaction;

/**
 * Kind of a recorded {@link Transaction}.
 */
public enum TransactionType {
    /** The account was opened with the point's default balance. */
    INITIAL,
    ADD,
    SUBTRACT,
    /** The balance was replaced; the amount is the difference to the previous balance. */
    SET,
    /** The sender side of a transfer. */
    TRANSFER_OUT,
    /** The receiver side of a transfer. */
    TRANSFER_IN,
    /** The balance was imported from InfPoints 2.x data. */
    MIGRATION
}
