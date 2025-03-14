package org.bigcraft.infpoints.core.factory;

import org.bigcraft.infpoints.core.JsonPoint;
import org.bigcraft.infpoints.core.Point;
import org.bukkit.configuration.ConfigurationSection;

public class JsonPointFactory implements PointFactory {
    @Override
    public Point create(ConfigurationSection config) {
        return new JsonPoint(config);
    }
}
