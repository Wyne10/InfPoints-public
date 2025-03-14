package org.bigcraft.infpoints.core.factory;

import org.bigcraft.infpoints.core.Point;
import org.bigcraft.infpoints.core.XpPoint;
import org.bukkit.configuration.ConfigurationSection;

public class XpPointFactory implements PointFactory {
    @Override
    public Point create(ConfigurationSection config) {
        return new XpPoint(config);
    }
}
