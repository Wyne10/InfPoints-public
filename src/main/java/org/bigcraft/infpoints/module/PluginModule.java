package org.bigcraft.infpoints.module;

import com.google.inject.AbstractModule;
import org.bigcraft.infpoints.InfPoints;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class PluginModule extends AbstractModule {

    private final InfPoints plugin;

    public PluginModule(InfPoints plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(InfPoints.class)
                .toInstance(plugin);
        bind(JavaPlugin.class)
                .toInstance(plugin);
        bind(FileConfiguration.class)
                .toInstance(plugin.getConfig());
    }

}
