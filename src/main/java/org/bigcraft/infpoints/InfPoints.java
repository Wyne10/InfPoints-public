package org.bigcraft.infpoints;

import com.google.inject.Injector;
import me.wyne.wutils.common.plugin.CompositeJavaPlugin;
import org.bukkit.configuration.MemoryConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public final class InfPoints extends CompositeJavaPlugin<InfPoints> {

    public static final MemoryConfiguration EMPTY_CONFIGURATION = new MemoryConfiguration();

    private static InfPoints instance;
    private static Logger logger;

    private @Nullable Injector injector;

    @Override
    public void init() {
        instance = this;
        logger = getSLF4JLogger();
        addSteps(
                new PluginSteps.LoadDefaultConfig(),
                new PluginSteps.InitializeLogger(),
                new PluginSteps.MigrateConfig(),
                new PluginSteps.InitializeI18n(),
                new PluginSteps.InitializeInjector(),
                new PluginSteps.InitializeConfig(),
                new PluginSteps.InitializeLoader(),
                new PluginSteps.Load(),
                new PluginSteps.Disable(),
                new PluginSteps.Reload()
        );
    }

    public static @NotNull InfPoints getInstance() {
        return instance;
    }

    public static @NotNull Logger logger() {
        return logger;
    }

    static void setLogger(@NotNull Logger logger) {
        InfPoints.logger = logger;
    }

    public @Nullable Injector getInjector() {
        return injector;
    }

    void setInjector(@Nullable Injector injector) {
        this.injector = injector;
    }

}
