package me.wyne.infpoints.point;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.wyne.wutils.common.loadable.Loadable;
import me.wyne.wutils.common.loadable.LoadableMeta;
import me.wyne.wutils.common.loadable.Loader;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Singleton
@LoadableMeta(priority = 1)
public final class PermissionRegistrar implements Loadable {

    private static final List<String> ACTIONS = List.of("balance", "balance-other", "set", "add", "sub", "pay", "exchange", "history", "history-other");

    private final PointManager points;
    private final Set<String> registered = new HashSet<>();

    @Inject
    public PermissionRegistrar(PointManager points) {
        this.points = points;
        Loader.global.registerLoadable(this);
    }

    @Override
    public void load(@NotNull ConfigurationSection config) {
        PluginManager manager = Bukkit.getPluginManager();
        Set<String> nodes = new HashSet<>();
        for (String action : ACTIONS) {
            Permission parent = manager.getPermission("points." + action + ".*");
            if (parent == null) {
                parent = new Permission("points." + action + ".*", PermissionDefault.OP);
                manager.addPermission(parent);
            }
            parent.getChildren().clear();
            for (String key : points.getKeys()) {
                String node = "points." + action + "." + key;
                nodes.add(node);
                parent.getChildren().put(node, true);
                if (manager.getPermission(node) == null)
                    manager.addPermission(new Permission(node, PermissionDefault.OP));
            }
            parent.recalculatePermissibles();
        }
        for (String node : registered) {
            if (!nodes.contains(node))
                manager.removePermission(node);
        }
        registered.clear();
        registered.addAll(nodes);
    }

}
