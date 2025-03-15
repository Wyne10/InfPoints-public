package org.bigcraft.infpoints;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bigcraft.infpoints.api.*;
import org.bigcraft.infpoints.core.PointManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

@Singleton
public class InfPointsApi implements PointApi {

    private final InfPoints plugin;
    private final PointManager pointManager;

    @Inject
    public InfPointsApi(InfPoints plugin, PointManager pointManager) {
        this.plugin = plugin;
        this.pointManager = pointManager;
        plugin.getServer().getServicesManager().register(PointApi.class, this, plugin, ServicePriority.Normal);
        IPApi.setInstance(this);
    }

    @Override
    public JavaPlugin getPlugin() {
        return plugin;
    }

    @Override
    public PointProvider getPointProvider() {
        return pointManager;
    }

    @Override
    public @Nullable PointType getPoint(String key) {
        return pointManager.getPoint(key);
    }

    @Override
    public @Nullable PointConfig getPointConfig(String key) {
        return pointManager.getPointConfig(key);
    }

}
