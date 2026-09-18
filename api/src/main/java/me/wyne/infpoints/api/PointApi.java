package me.wyne.infpoints.api;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point to the InfPoints API for a running server.
 * <p>
 * Obtain the active instance via {@link IPApi#getInstance()}, or through the
 * Bukkit services manager registered under this interface.
 */
public interface PointApi extends PointProvider {

    /**
     * Returns the InfPoints plugin instance.
     */
    JavaPlugin getPlugin();

    /**
     * Returns the underlying {@link PointProvider} backing this API.
     * <p>
     * This instance's own {@link PointProvider} methods delegate to it, so
     * calling them directly is equivalent.
     */
    PointProvider getPointProvider();

}
