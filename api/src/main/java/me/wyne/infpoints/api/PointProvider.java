package me.wyne.infpoints.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

/**
 * Registry of configured {@link Point}s, keyed by their configuration key.
 */
public interface PointProvider {

    /**
     * Returns the keys of all configured points, in configuration order.
     */
    @NotNull Set<String> getKeys();

    /**
     * Returns the point configured under {@code key}, or {@code null} if no point with that key is configured.
     */
    @Nullable Point getPoint(@NotNull String key);

    /**
     * Returns all configured points, in configuration order.
     */
    @NotNull Collection<Point> getPoints();

}
