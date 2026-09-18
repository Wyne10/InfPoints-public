package me.wyne.infpoints;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.wyne.wutils.common.terminable.Terminable;
import me.wyne.infpoints.api.IPApi;
import me.wyne.infpoints.api.Point;
import me.wyne.infpoints.api.PointApi;
import me.wyne.infpoints.api.PointProvider;
import me.wyne.infpoints.point.PointManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

@Singleton
public final class InfPointsApi implements PointApi, Terminable {

    private final InfPoints plugin;
    private final PointManager points;

    @Inject
    public InfPointsApi(InfPoints plugin, PointManager points) {
        this.plugin = plugin;
        this.points = points;
        Bukkit.getServicesManager().register(PointApi.class, this, plugin, ServicePriority.Normal);
        IPApi.setInstance(this);
        plugin.bind(this);
    }

    @Override
    public @NotNull JavaPlugin getPlugin() {
        return plugin;
    }

    @Override
    public @NotNull PointProvider getPointProvider() {
        return points;
    }

    @Override
    public @NotNull Set<String> getKeys() {
        return points.getKeys();
    }

    @Override
    public @Nullable Point getPoint(@NotNull String key) {
        return points.getPoint(key);
    }

    @Override
    public @NotNull Collection<Point> getPoints() {
        return points.getPoints();
    }

    @Override
    public void close() {
        Bukkit.getServicesManager().unregister(PointApi.class, this);
        IPApi.setInstance(null);
    }

}
