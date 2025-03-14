package org.bigcraft.infpoints.core;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import org.bigcraft.infpoints.InfPoints;
import org.bigcraft.infpoints.core.factory.PointFactory;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class PointManager {

    @Getter private final Map<String, Point> points = new HashMap<>();

    private final InfPoints plugin;
    private final Map<String, PointFactory> pointTypeMap;

    @Inject
    public PointManager(InfPoints plugin, Map<String, PointFactory> pointTypeMap) {
        this.plugin = plugin;
        this.pointTypeMap = pointTypeMap;
    }

    public void loadPoints() {
        for (String pointKey : plugin.getConfig().getConfigurationSection("points").getKeys(false)) {
            ConfigurationSection point = plugin.getConfig().getConfigurationSection("points." + pointKey);
            String type = point.getString("type");
            if (!pointTypeMap.containsKey(type))
                throw new IllegalArgumentException("Unknown point type " + type);
            points.put(pointKey, pointTypeMap.get(type).create(point));
        }
    }

}
