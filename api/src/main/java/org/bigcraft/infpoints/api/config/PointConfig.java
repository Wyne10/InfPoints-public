package org.bigcraft.infpoints.api.config;

import org.bigcraft.infpoints.api.PointTypes;
import org.bukkit.configuration.ConfigurationSection;

public record PointConfig(String key, String type, PointTypes pointType, double defaultBalance) {

    public static PointConfig fromConfig(ConfigurationSection section) {
        String key = section.getName();
        String type = section.getString("type");
        PointTypes pointType = PointTypes.valueOf(type);
        double defaultBalance = section.getLong("defaultBalance");
        return new PointConfig(key, type, pointType, defaultBalance);
    }

}
