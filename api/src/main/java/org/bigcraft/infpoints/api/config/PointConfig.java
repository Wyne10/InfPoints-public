package org.bigcraft.infpoints.api.config;

import org.bukkit.configuration.ConfigurationSection;

public record PointConfig(String key, String type, double defaultBalance) {

    public static PointConfig fromConfig(ConfigurationSection section) {
        String key = section.getName();
        String type = section.getString("type");
        double defaultBalance = section.getLong("defaultBalance");
        return new PointConfig(key, type, defaultBalance);
    }

}
