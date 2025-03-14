package org.bigcraft.infpoints.core.factory;

import org.bigcraft.infpoints.core.MemoryPoint;
import org.bigcraft.infpoints.core.Point;
import org.bukkit.configuration.ConfigurationSection;

public class MemoryPointFactory implements PointFactory {
    @Override
    public Point create(ConfigurationSection config) {
        return new MemoryPoint(config);
    }
}
