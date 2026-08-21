package org.bigcraft.infpoints.api;

import org.bigcraft.infpoints.api.config.CommandConfig;
import org.bigcraft.infpoints.api.config.VisualConfig;

/**
 * Configuration of a single point (currency) type.
 */
public interface PointConfig {

    /**
     * Returns the raw configuration (key, storage type and default balance) for this point.
     */
    org.bigcraft.infpoints.api.config.PointConfig getConfig();

    /**
     * Returns the display configuration (name, symbol, color and number format) for this point.
     */
    VisualConfig getVisualConfig();

    /**
     * Returns the pay/balance command configuration for this point.
     */
    CommandConfig getCommandConfig();

}
