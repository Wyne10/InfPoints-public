package org.bigcraft.infpoints;

import com.google.gson.GsonBuilder;
import com.google.inject.*;
import com.j256.ormlite.logger.Logger;
import lombok.Getter;
import me.wyne.wutils.config.Config;
import me.wyne.wutils.i18n.I18n;
import me.wyne.wutils.i18n.PluginI18nBuilder;
import me.wyne.wutils.i18n.language.component.BukkitComponentAudiences;
import me.wyne.wutils.i18n.language.interpretation.ComponentInterpreters;
import me.wyne.wutils.i18n.language.validation.EmptyValidator;
import me.wyne.wutils.jdbc.DriverLibrary;
import me.wyne.wutils.json.JsonRegistry;
import me.wyne.wutils.log.*;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bigcraft.infpoints.config.SqlConfig;
import org.bigcraft.infpoints.core.PointManager;
import org.bigcraft.infpoints.module.*;
import org.bigcraft.infpoints.sql.ConnectionProvider;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.concurrent.Executors;

@Singleton
public class InfPoints extends JavaPlugin {

    @Getter private static InfPoints instance;
    @Getter private org.slf4j.Logger log;

    private Injector injector;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        getConfig().setDefaults(new MemoryConfiguration());

        initializeLogger();
        initializeJson();
        initializeI18n();

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
            log.error("Guice injector creation exception", e);
        }

        initializeConfig();

        try {
            injector.getInstance(PointManager.class).loadPoints();
            injector.getInstance(PointManager.class).registerPermissions();
            injector.getInstance(PointManager.class).implementVault();
            JsonRegistry.global.load();
        } catch (ConfigurationException | ProvisionException e) {
            log.error("Guice configuration/provision exception", e);
        } catch (IOException | IllegalAccessException e) {
            log.error("Json load error", e);
        }
    }

    @Override
    public void onDisable() {
        try {
            injector.getInstance(PointManager.class).close();
            injector.getInstance(ConnectionProvider.class).close();
            JsonRegistry.global.write();
        } catch (ConfigurationException | ProvisionException e) {
            log.error("Guice configuration/provision exception", e);
        } catch (IOException | IllegalAccessException e) {
            log.error("Json write error", e);
        }
    }

    private void initializeLogger()
    {
        Logger.setGlobalLogLevel(com.j256.ormlite.logger.Level.INFO);
        Log.global = Log.builder()
                .setLogger(getLogger())
                .setLevel(JulLevel.valueOf(getConfig().getString("logLevel", "INFO")).getLevel())
                .setLogDirectory(new File(getDataFolder(), "log"))
                .setFileWriteExecutor(Executors.newSingleThreadExecutor())
                .build();
        Log.global.deleteOlderLogs();

        log = Log4jFactory.createLogger(
                this,
                Log4jFactory.DEFAULT_FILE_MESSAGE_PATTERN,
                Level.valueOf(getConfig().getString("logLevel", "INFO")),
                new File(getDataFolder(), "log").getPath(),
                Log.global
        );
    }

    private void initializeConfig()
    {
        Config.global.logger = log;
        Config.global.setConfigGenerator(this, "config.yml");
        Config.global.generateConfig();
        reloadConfig();
        getConfig().setDefaults(new MemoryConfiguration());
        Config.global.reloadConfig(getConfig());
    }

    private void initializeI18n()
    {
        I18n.global = new PluginI18nBuilder(this)
                .setLogger(log)
                .setComponentAudience(new BukkitComponentAudiences(BukkitAudiences.create(this)))
                .setComponentInterpreter(
                        ComponentInterpreters.valueOf(
                                getConfig().getString("serializer", "MINI_MESSAGE")
                        ).get(new EmptyValidator())
                )
                .setUsePlayerLanguage(getConfig().getBoolean("usePlayerLanguage", true))
                .loadLanguage("lang/ru.yml")
                .loadLanguage("lang/en.yml")
                .build();
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
            JsonRegistry.global.clear();
            injector.getInstance(PointManager.class).close();
            DriverLibrary.valueOf(injector.getInstance(SqlConfig.class).getDriver()).registerDriver();
            injector.getInstance(ConnectionProvider.class).reloadConnectionPool();
            injector.getInstance(PointManager.class).loadPoints();
            injector.getInstance(PointManager.class).implementVault();
            JsonRegistry.global.load();
        } catch (ConfigurationException | ProvisionException e) {
            log.error("Guice configuration/provision exception", e);
        } catch (SQLException | IOException | ClassNotFoundException | InvocationTargetException |
                 IllegalAccessException | NoSuchMethodException | InstantiationException e) {
            log.error("Reload error", e);
        }
    }

}
