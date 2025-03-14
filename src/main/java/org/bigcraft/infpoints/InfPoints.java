package org.bigcraft.infpoints;

import com.google.inject.*;
import me.wyne.wutils.config.Config;
import me.wyne.wutils.i18n.I18n;
import me.wyne.wutils.i18n.language.interpretation.ComponentInterpreters;
import me.wyne.wutils.i18n.language.validation.EmptyValidator;
import me.wyne.wutils.log.BasicLogConfig;
import me.wyne.wutils.log.ConfigurableLogConfig;
import me.wyne.wutils.log.Log;
import org.bigcraft.infpoints.core.PointManager;
import org.bigcraft.infpoints.module.*;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.concurrent.Executors;

@Singleton
public class InfPoints extends JavaPlugin {

    private Injector injector;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().setDefaults(new MemoryConfiguration());

        initializeLogger();
        initializeI18n();

        try {
            injector =  Guice.createInjector(
                    Stage.PRODUCTION,
                    new PluginModule(this),
                    new CoreModule(),
                    new PointTypeModule(),
                    new PlaceholderModule(),
                    new CommandModule()
            );
        } catch (CreationException e) {
            Log.global.exception("Guice injector creation exception", e);
        }

        initializeConfig();

        try {
            injector.getInstance(PointManager.class).loadPoints();
        } catch (ConfigurationException | ProvisionException e) {
            Log.global.exception("Guice configuration/provision exception", e);
        }
    }

    private void initializeLogger()
    {
        Log.global = Log.builder()
                .setLogger(getLogger())
                .setConfig(new ConfigurableLogConfig("Global", Config.global, new BasicLogConfig(true, true, true, true)))
                .setLogDirectory(new File(getDataFolder(), "log"))
                .setFileWriteExecutor(Executors.newSingleThreadExecutor())
                .build();
        Log.global.deleteOlderLogs();
    }

    private void initializeConfig()
    {
        Config.global.setConfigGenerator(this, "config.yml");
        Config.global.generateConfig();
        reloadConfig();
        getConfig().setDefaults(new MemoryConfiguration());
        Config.global.reloadConfig(getConfig());
    }

    private void initializeI18n()
    {
        I18n.global.clearLanguageMap();
        I18n.global.loadLanguage("lang/ru.yml", this);
        I18n.global.loadLanguage("lang/en.yml", this);
        I18n.global.loadDefaultResourceLanguage(this);
        I18n.global.loadLanguages(this);
        I18n.global.setDefaultLanguage(I18n.global.getDefaultLanguageCode(this));
        I18n.global.setComponentInterpreter(ComponentInterpreters.valueOf(getConfig().getString("serializer", "LEGACY")).get(new EmptyValidator()));
        I18n.global.setUsePlayerLanguage(getConfig().getBoolean("usePlayerLanguage", true));
    }

    public void reload()
    {
        reloadConfig();
        getConfig().setDefaults(new MemoryConfiguration());
        Config.global.reloadConfig(getConfig());
        initializeI18n();
        try {
            // TODO Reload model
        } catch (ConfigurationException | ProvisionException e) {
            Log.global.exception("Guice configuration/provision exception", e);
        }
    }

}
