package org.bigcraft.infpoints.core;

import lombok.Getter;
import org.bigcraft.infpoints.api.PointType;
import org.bigcraft.infpoints.config.CommandConfig;
import org.bigcraft.infpoints.config.PointConfig;
import org.bigcraft.infpoints.config.VisualConfig;
import org.bukkit.configuration.ConfigurationSection;

@Getter
public abstract class Point implements PointType {

    private final PointConfig config;
    private final VisualConfig visualConfig;
    private final CommandConfig commandConfig;

    public Point(ConfigurationSection config) {
        this.config = PointConfig.fromConfig(config);
        this.visualConfig = VisualConfig.fromConfig(config);
        this.commandConfig = CommandConfig.fromConfig(config);
    }

}
