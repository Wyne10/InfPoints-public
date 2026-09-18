package me.wyne.infpoints.api.config;

import me.wyne.infpoints.api.PointStorageType;
import org.jetbrains.annotations.NotNull;

/**
 * Storage configuration of a single point.
 *
 * @param key            the point's configuration key, also used to look it up via
 *                       {@link me.wyne.infpoints.api.PointProvider}
 * @param type           the storage backend
 * @param defaultBalance the balance a player has before any change was applied to it
 * @param decimals       the number of decimal places balances are exact to; amounts are rounded half-up to it
 */
public record PointConfig(@NotNull String key, @NotNull PointStorageType type, double defaultBalance, int decimals) {
}
