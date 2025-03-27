package org.bigcraft.infpoints;

import com.google.gson.GsonBuilder;
import com.google.inject.*;
import com.j256.ormlite.logger.Level;
import com.j256.ormlite.logger.Logger;
import lombok.Getter;
import me.wyne.wutils.config.Config;
import me.wyne.wutils.i18n.I18n;
import me.wyne.wutils.i18n.language.interpretation.ComponentInterpreters;
import me.wyne.wutils.i18n.language.validation.EmptyValidator;
import me.wyne.wutils.jdbc.DriverLibrary;
import me.wyne.wutils.json.JsonRegistry;
import me.wyne.wutils.log.BasicLogConfig;
import me.wyne.wutils.log.ConfigurableLogConfig;
import me.wyne.wutils.log.Log;
import org.bigcraft.infpoints.config.SqlConfig;
import org.bigcraft.infpoints.core.PointManager;
import org.bigcraft.infpoints.module.*;
import org.bigcraft.infpoints.sql.ConnectionProvider;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.text.DecimalFormat;
import java.util.concurrent.Executors;

@Singleton
public class InfPoints extends JavaPlugin {

    private Injector injector;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().setDefaults(new MemoryConfiguration());

        initializeJson();
        initializeLogger();
        initializeI18n();

        Logger.setGlobalLogLevel(Level.INFO);

        try {
            injector =  Guice.createInjector(
                    Stage.PRODUCTION,
                    new PluginModule(this),
                    new CoreModule(),
                    new ConfigModule(),
                    new PointTypeModule(),
                    new PlaceholderModule(),
                    new CommandModule(),
                    new ApiModule()
            );
        } catch (CreationException e) {
            Log.global.exception("Guice injector creation exception", e);
        }

        initializeConfig();

        try {
            injector.getInstance(PointManager.class).loadPoints();
            injector.getInstance(PointManager.class).registerPermissions();
            injector.getInstance(PointManager.class).implementVault();
            JsonRegistry.global.load();
        } catch (ConfigurationException | ProvisionException e) {
            Log.global.exception("Guice configuration/provision exception", e);
        }
    }

    @Override
    public void onDisable() {
        try {
            injector.getInstance(ConnectionProvider.class).close();
            JsonRegistry.global.write();
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

    private void initializeJson()
    {
        JsonRegistry.global.setGson(new GsonBuilder().setPrettyPrinting().create());
        JsonRegistry.global.setDirectory(getDataFolder());
    }

    public void reload()
    {
        reloadConfig();
        getConfig().setDefaults(new MemoryConfiguration());
        Config.global.reloadConfig(getConfig());
        initializeI18n();
        try {
            JsonRegistry.global.write();
            JsonRegistry.global.load();
            DriverLibrary.valueOf(injector.getInstance(SqlConfig.class).getDriver()).registerDriver();
            injector.getInstance(ConnectionProvider.class).reloadConnectionPool();
            injector.getInstance(PointManager.class).loadPoints();
            injector.getInstance(PointManager.class).implementVault();
        } catch (ConfigurationException | ProvisionException e) {
            Log.global.exception("Guice configuration/provision exception", e);
        }
    }

}
