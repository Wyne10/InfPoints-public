package org.bigcraft.infpoints.api.config;

import org.bukkit.configuration.ConfigurationSection;

public record PointConfig(String key, String type) {

    public static PointConfig fromConfig(ConfigurationSection section) {
        String key = section.getName();
        String type = section.getString("type");
        return new PointConfig(key, type);
    }

}
