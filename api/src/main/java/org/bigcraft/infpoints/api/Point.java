package org.bigcraft.infpoints.api;

import java.util.UUID;

/**
 * A single configured point (currency) type: combines balance access
 * ({@link PointType}), player-facing operations ({@link PointView}) and its
 * configuration ({@link PointConfig}).
 * <p>
 * Instances are looked up by key through {@link PointProvider#getPoint(String)}.
 */
public interface Point extends PointType, PointConfig, PointView {
    /**
     * Formats the player's current balance using this point's configured
     * {@link org.bigcraft.infpoints.api.config.VisualConfig#decimalFormat()}.
     */
    String getFormat(UUID player);
}
