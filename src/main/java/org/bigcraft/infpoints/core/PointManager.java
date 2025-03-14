package org.bigcraft.infpoints.core;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import org.bigcraft.infpoints.InfPoints;
import org.bigcraft.infpoints.core.factory.PointFactory;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.permissions.Permission;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class PointManager {

    private final static String[] permissions = {"points.balance.", "points.balance-other.", "points.set.", "points.add.", "points.sub.", "points.pay."};

    @Getter private final Map<String, Point> points = new HashMap<>();

    private final InfPoints plugin;
    private final Map<String, PointFactory> pointTypeMap;

    @Inject
    public PointManager(InfPoints plugin, Map<String, PointFactory> pointTypeMap) {
        this.plugin = plugin;
        this.pointTypeMap = pointTypeMap;
    }

    public void loadPoints() {
        points.clear();
        for (String pointKey : plugin.getConfig().getConfigurationSection("points").getKeys(false)) {
            ConfigurationSection point = plugin.getConfig().getConfigurationSection("points." + pointKey);
            String type = point.getString("type");
            if (!pointTypeMap.containsKey(type))
                throw new IllegalArgumentException("Unknown point type " + type);
            points.put(pointKey, pointTypeMap.get(type).create(point));
        }
    }

    public void registerPermissions() {
        points.keySet().forEach((key) -> {
            for (String permission : permissions) {
                Bukkit.getPluginManager().addPermission(new Permission(permission + key));
            }
        });
    }

}
