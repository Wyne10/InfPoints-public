package org.bigcraft.infpoints.core;

import lombok.Getter;
import org.bigcraft.infpoints.api.PointConfig;
import org.bigcraft.infpoints.api.PointType;
import org.bigcraft.infpoints.api.config.CommandConfig;
import org.bigcraft.infpoints.api.config.VisualConfig;
import org.bukkit.configuration.ConfigurationSection;

@Getter
public abstract class Point implements PointType, PointConfig {

    private final org.bigcraft.infpoints.api.config.PointConfig config;
    private final VisualConfig visualConfig;
    private final CommandConfig commandConfig;

    public Point(ConfigurationSection config) {
        this.config = org.bigcraft.infpoints.api.config.PointConfig.fromConfig(config);
        this.visualConfig = VisualConfig.fromConfig(config);
        this.commandConfig = CommandConfig.fromConfig(config);
    }

    protected Point(Point point) {
        this.config = point.config;
        this.visualConfig = point.visualConfig;
        this.commandConfig = point.commandConfig;
    }

}
