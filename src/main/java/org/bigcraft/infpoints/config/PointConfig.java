package org.bigcraft.infpoints.config;

import org.bukkit.configuration.ConfigurationSection;

public record PointConfig(String type) {

    public static PointConfig fromConfig(ConfigurationSection section) {
        String type = section.getString("type");
        return new PointConfig(type);
    }

}
