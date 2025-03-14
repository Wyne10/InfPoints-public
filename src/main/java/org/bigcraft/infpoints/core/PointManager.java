package org.bigcraft.infpoints.core;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import org.bigcraft.infpoints.InfPoints;
import org.bigcraft.infpoints.command.BalanceCommand;
import org.bigcraft.infpoints.command.PayCommand;
import org.bigcraft.infpoints.command.PersonalCommand;
import org.bigcraft.infpoints.core.factory.PointFactory;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.permissions.Permission;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Singleton
public class PointManager {

    private final static String[] permissions = {"points.balance.", "points.balance-other.", "points.set.", "points.add.", "points.sub.", "points.pay."};

    @Getter private final Map<String, Point> points = new HashMap<>();
    private final Set<PersonalCommand> personalCommands = new HashSet<>();

    private final InfPoints plugin;
    private final Map<String, PointFactory> pointTypeMap;

    @Inject
    public PointManager(InfPoints plugin, Map<String, PointFactory> pointTypeMap) {
        this.plugin = plugin;
        this.pointTypeMap = pointTypeMap;
    }

    public void loadPoints() {
        points.clear();
        personalCommands.forEach(PersonalCommand::unregister);
        personalCommands.clear();
        for (String pointKey : plugin.getConfig().getConfigurationSection("points").getKeys(false)) {
            ConfigurationSection pointConfig = plugin.getConfig().getConfigurationSection("points." + pointKey);
            String type = pointConfig.getString("type");
            if (!pointTypeMap.containsKey(type))
                throw new IllegalArgumentException("Unknown point type " + type);
            Point point = pointTypeMap.get(type).create(pointConfig);
            points.put(pointKey, point);
            if (point.getCommandConfig().payCommand() != null)
                personalCommands.add(new PayCommand(points.get(pointKey)));
            if (point.getCommandConfig().balanceCommand() != null)
                personalCommands.add(new BalanceCommand(points.get(pointKey)));
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
