package org.bigcraft.infpoints.api.config;

import org.bukkit.configuration.ConfigurationSection;

public record VisualConfig(String name, String pluralName, String symbol, String color) {

    public static VisualConfig fromConfig(ConfigurationSection section) {
        String name = section.getString("name", "");
        String pluralName = section.getString("pluralName", "");
        String symbol = section.getString("symbol", "");
        String color = section.getString("color", "");
        return new VisualConfig(name, pluralName, symbol, color);
    }

}
