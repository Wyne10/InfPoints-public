package me.wyne.infpoints.api;

/**
 * Storage backends available for a configured point.
 */
public enum PointStorageType {
    /** Balances kept in memory only, lost on restart. No history. */
    MEMORY,
    /** Balances stored in the player's persistent data container. Online players only, no history. */
    PDC,
    /** Balance backed by the player's vanilla experience level. Online players only, no history. */
    LEVEL,
    /** Balance backed by the player's vanilla experience points. Online players only, no history. */
    EXP,
    /**
     * Balances and transaction history stored in the ConnectionSource database. Safe for several servers
     * sharing one database.
     */
    SQL
}
