package org.bigcraft.infpoints.api;

/**
 * Storage backends available for a configured point type.
 */
public enum PointTypes {
    /** In-memory storage; balances are not persisted across restarts. */
    MEMORY,
    /** Balances persisted to a JSON file. */
    JSON,
    /** Balances persisted in the player's Persistent Data Container. */
    PDC,
    /** Balance backed by the player's in-game experience level. */
    LEVEL,
    /** Balance backed by the player's in-game experience points. */
    EXP,
    /** Balances persisted in an SQL database. */
    SQL
}
