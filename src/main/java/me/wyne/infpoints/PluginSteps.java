package me.wyne.infpoints;

import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import me.wyne.wutils.common.loadable.Loader;
import me.wyne.wutils.common.plugin.CompositeStep;
import me.wyne.wutils.common.plugin.LevelWrapper;
import me.wyne.wutils.common.plugin.LoggerWrapper;
import me.wyne.wutils.common.plugin.PluginStep;
import me.wyne.wutils.common.plugin.Step;
import me.wyne.wutils.common.plugin.StepScope;
import me.wyne.wutils.config.Config;
import me.wyne.wutils.i18n.I18n;
import me.wyne.wutils.i18n.PluginI18nBuilder;
import me.wyne.wutils.i18n.language.component.BukkitComponentAudiences;
import me.wyne.wutils.i18n.language.interpretation.ComponentInterpreters;
import me.wyne.wutils.i18n.language.validation.EmptyValidator;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import me.wyne.infpoints.module.ConnectionModule;
import me.wyne.infpoints.module.CoreModules;
import me.wyne.infpoints.module.OptionalModules;
import me.wyne.infpoints.module.PluginModule;
import me.wyne.infpoints.point.TransactionExecutor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class PluginSteps {

    private PluginSteps() {}

    @Step(priority = 0, scope = StepScope.ENABLE)
    public static final class LoadDefaultConfig implements PluginStep<InfPoints> {
        @Override
        public void run(@NotNull InfPoints plugin) {
            plugin.saveDefaultConfig();
            plugin.getConfig().setDefaults(InfPoints.EMPTY_CONFIGURATION);
        }
    }

    @Step(priority = 1, scope = StepScope.ENABLE)
    public static final class InitializeLogger implements PluginStep<InfPoints> {
        @Override
        public void run(@NotNull InfPoints plugin) {
            String level = plugin.getConfig().getString("logLevel", "INFO");
            LevelWrapper levelWrapper;
            try {
                levelWrapper = LevelWrapper.valueOf(level);
            } catch (IllegalArgumentException e) {
                levelWrapper = LevelWrapper.INFO;
            }
            InfPoints.setLogger(new LoggerWrapper(plugin.getSLF4JLogger(), levelWrapper));
        }
    }

    // Upgrades a 2.x config.yml once: its Vault keys collide with the generated 'vault' section, and it needs
    // regenerating to receive the new sections. Saving drops comments, so the original file is backed up first
    @Step(priority = 2, scope = StepScope.ENABLE)
    public static final class MigrateConfig implements PluginStep<InfPoints> {
        @Override
        public void run(@NotNull InfPoints plugin) {
            FileConfiguration config = plugin.getConfig();
            boolean legacyVault = config.contains("implementVault") || config.isString("vault");
            boolean legacy = legacyVault || (config.isConfigurationSection("sql") && !config.isConfigurationSection("storage"));
            if (legacy) {
                File configFile = new File(plugin.getDataFolder(), "config.yml");
                File backup = new File(plugin.getDataFolder(), "backups/config-2.x.yml");
                try {
                    Files.createDirectories(backup.toPath().getParent());
                    Files.copy(configFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    InfPoints.logger().error("Couldn't back up the InfPoints 2.x config.yml, it is left unchanged", e);
                    return;
                }
                if (legacyVault) {
                    boolean enabled = config.getBoolean("implementVault", false);
                    String point = config.isString("vault") ? config.getString("vault") : "primary";
                    config.set("implementVault", null);
                    config.set("vault", null);
                    config.set("vault.enabled", enabled);
                    config.set("vault.point", point);
                }
                config.set("regenerate", true);
                plugin.saveConfig();
                plugin.reloadConfig();
                plugin.getConfig().setDefaults(InfPoints.EMPTY_CONFIGURATION);
                InfPoints.logger().info("Updated the InfPoints 2.x config.yml, the original was saved as backups/config-2.x.yml");
            }
            if (plugin.getConfig().isConfigurationSection("sql"))
                InfPoints.logger().warn("The 'sql' section is no longer used, SQL points use the ConnectionSource database. Remove the section from config.yml");
        }
    }

    @Step(priority = 3, scope = StepScope.ENABLE)
    public static final class InitializeI18n implements PluginStep<InfPoints> {

        private static @Nullable BukkitAudiences audiences;

        @Override
        public void run(@NotNull InfPoints plugin) {
            if (audiences != null)
                audiences.close();
            audiences = BukkitAudiences.create(plugin);
            String serializer = plugin.getConfig().getString("serializer", "MINI_MESSAGE");
            ComponentInterpreters interpreter;
            try {
                interpreter = ComponentInterpreters.valueOf(serializer);
            } catch (IllegalArgumentException e) {
                InfPoints.logger().error("Unknown serializer '{}', using MINI_MESSAGE", serializer);
                interpreter = ComponentInterpreters.MINI_MESSAGE;
            }
            I18n.global = new PluginI18nBuilder(plugin)
                    .setLogger(InfPoints.logger())
                    .setComponentAudience(new BukkitComponentAudiences(audiences))
                    .setComponentInterpreter(interpreter.get(new EmptyValidator()))
                    .setUsePlayerLanguage(plugin.getConfig().getBoolean("usePlayerLanguage", true))
                    .loadLanguage("lang/ru.yml")
                    .loadLanguage("lang/en.yml")
                    .build();
        }
    }

    @Step(priority = 4, scope = StepScope.ENABLE)
    public static final class InitializeInjector implements PluginStep<InfPoints> {
        @Override
        public void run(@NotNull InfPoints plugin) {
            try {
                plugin.setInjector(Guice.createInjector(
                        Stage.PRODUCTION,
                        new PluginModule(plugin),
                        new ConnectionModule(),
                        CoreModules.POINT,
                        CoreModules.STORAGE,
                        CoreModules.API,
                        OptionalModules.PLACEHOLDER,
                        OptionalModules.COMMAND,
                        OptionalModules.VAULT
                ));
            } catch (CreationException e) {
                InfPoints.logger().error("Guice injector creation exception, disabling InfPoints", e);
                Bukkit.getPluginManager().disablePlugin(plugin);
            }
        }
    }

    @Step(priority = 5, scope = StepScope.ENABLE)
    public static final class InitializeConfig extends CompositeStep<InfPoints> {

        public InitializeConfig() {
            super(new ReloadConfig());
        }

        @Override
        public void before(@NotNull InfPoints plugin) {
            Config.global.logger = InfPoints.logger();
            Config.global.setConfigGenerator(plugin, "config.yml");
            Config.global.generateConfig();
        }
    }

    @Step(priority = 6, scope = StepScope.ENABLE)
    public static final class InitializeLoader implements PluginStep<InfPoints> {
        @Override
        public void run(@NotNull InfPoints plugin) {
            Loader.global.registerConfig(Loader.DEFAULT_PATH, plugin.getConfig());
        }
    }

    @Step(priority = 7, scope = StepScope.ENABLE)
    public static final class Load implements PluginStep<InfPoints> {
        @Override
        public void run(@NotNull InfPoints plugin) {
            if (plugin.getInjector() == null)
                return;
            Loader.global.load(plugin);
        }
    }

    // Runs before the terminable registry closes, so queued transactions are applied while storages still work
    @Step(scope = StepScope.DISABLE)
    public static final class Disable implements PluginStep<InfPoints> {
        @Override
        public void run(@NotNull InfPoints plugin) {
            Injector injector = plugin.getInjector();
            if (injector != null)
                injector.getInstance(TransactionExecutor.class).close();
        }
    }

    @Step(scope = StepScope.RELOAD)
    public static final class Reload extends CompositeStep<InfPoints> {
        public Reload() {
            super(new ReloadConfig(), new InitializeLogger(), new InitializeI18n(), new InitializeLoader(), new Load());
        }
    }

    public static final class ReloadConfig implements PluginStep<InfPoints> {
        @Override
        public void run(@NotNull InfPoints plugin) {
            plugin.reloadConfig();
            plugin.getConfig().setDefaults(InfPoints.EMPTY_CONFIGURATION);
            Config.global.reloadConfig(plugin.getConfig());
        }
    }

}
