package org.bigcraft.infpoints.api;

import org.bigcraft.infpoints.api.config.CommandConfig;
import org.bigcraft.infpoints.api.config.VisualConfig;

public interface PointConfig {

    org.bigcraft.infpoints.api.config.PointConfig getConfig();
    VisualConfig getVisualConfig();
    CommandConfig getCommandConfig();

}
