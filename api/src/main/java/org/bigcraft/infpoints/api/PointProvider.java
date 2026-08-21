package org.bigcraft.infpoints.api;

import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Registry of configured {@link Point}s, keyed by their configuration key.
 */
public interface PointProvider {

    /**
     * Returns the keys of all configured points.
     */
    Set<String> getKeys();

    /**
     * Returns the point registered under {@code key}, or {@code null} if no point
     * with that key is configured.
     */
    @Nullable Point getPoint(String key);

    /**
     * Returns the {@link PointType} view of the point registered under {@code key},
     * or {@code null} if no point with that key is configured.
     */
    @Nullable PointType getPointType(String key);

    /**
     * Returns the {@link PointView} view of the point registered under {@code key},
     * or {@code null} if no point with that key is configured.
     */
    @Nullable PointView getPointView(String key);

    /**
     * Returns the {@link PointConfig} view of the point registered under {@code key},
     * or {@code null} if no point with that key is configured.
     */
    @Nullable PointConfig getPointConfig(String key);

}
