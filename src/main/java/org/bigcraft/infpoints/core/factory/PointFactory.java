package org.bigcraft.infpoints.core.factory;

import org.bigcraft.infpoints.core.Point;
import org.bukkit.configuration.ConfigurationSection;

@FunctionalInterface
public interface PointFactory {

    Point create(ConfigurationSection config);

}
