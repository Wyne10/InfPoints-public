package org.bigcraft.infpoints.core.factory;

import com.google.inject.Singleton;
import org.bigcraft.infpoints.core.Point;
import org.bigcraft.infpoints.core.XpPoint;
import org.bukkit.configuration.ConfigurationSection;

@Singleton
public class XpPointFactory implements PointFactory {

    private XpPoint xpPoint;

    @Override
    public Point create(ConfigurationSection config) {
        if (xpPoint != null)
            return xpPoint;
        this.xpPoint = new XpPoint(config);
        return xpPoint;
    }

}
