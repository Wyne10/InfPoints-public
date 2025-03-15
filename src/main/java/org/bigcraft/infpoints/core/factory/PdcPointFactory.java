package org.bigcraft.infpoints.core.factory;

import com.google.inject.Singleton;
import org.bigcraft.infpoints.core.PdcPoint;
import org.bigcraft.infpoints.core.Point;
import org.bukkit.configuration.ConfigurationSection;

@Singleton
public class PdcPointFactory implements PointFactory {
    @Override
    public Point create(ConfigurationSection config) {
        return new PdcPoint(config);
    }
}
