package me.wyne.infpoints;

import me.wyne.wutils.common.config.ConfigUtils;
import me.wyne.wutils.config.configurables.attribute.GenericFactory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DefinitionLoader<V> {

    private final String sectionKey;
    private final File directory;
    private final GenericFactory<V> factory;

    public DefinitionLoader(@NotNull String sectionKey, @NotNull File directory, @NotNull GenericFactory<V> factory) {
        this.sectionKey = sectionKey;
        this.directory = directory;
        this.factory = factory;
    }

    public @NotNull File getDirectory() {
        return directory;
    }

    public @NotNull Map<String, V> load(@NotNull ConfigurationSection config) {
        Map<String, V> loaded = new LinkedHashMap<>();
        ConfigurationSection section = config.getConfigurationSection(sectionKey);
        if (section != null) {
            for (String key : section.getKeys(false))
                create(loaded, key, ConfigUtils.getConfigurationSection(section, key), sectionKey);
        }
        File[] files = directory.listFiles((parent, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (files == null)
            return loaded;
        Arrays.sort(files);
        for (File file : files) {
            String key = file.getName().substring(0, file.getName().lastIndexOf('.'));
            String origin = directory.getName() + "/" + file.getName();
            // Inline definitions come from the existing configuration, so a file can never silently replace them
            if (section != null && section.contains(key)) {
                InfPoints.logger().error("'{}' is defined in both the '{}' section and '{}', the file is ignored", key, sectionKey, origin);
                continue;
            }
            create(loaded, key, ConfigUtils.getConfigurationSection(YamlConfiguration.loadConfiguration(file), key), origin);
        }
        return loaded;
    }

    private void create(Map<String, V> loaded, String key, ConfigurationSection config, String origin) {
        InfPoints.logger().debug("Loading '{}' from '{}'", key, origin);
        try {
            loaded.put(key, factory.create(key, config));
        } catch (IllegalArgumentException e) {
            InfPoints.logger().error("Failed loading '{}' from '{}': {}", key, origin, e.getMessage());
        } catch (RuntimeException e) {
            InfPoints.logger().error("Failed loading '{}' from '{}'", key, origin, e);
        }
    }

}
