package org.bigcraft.infpoints.api;

import org.bukkit.plugin.java.JavaPlugin;

public interface PointApi extends PointProvider {

    JavaPlugin getPlugin();
    PointProvider getPointProvider();

}
