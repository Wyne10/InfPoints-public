package org.bigcraft.infpoints.config;

import org.bukkit.configuration.ConfigurationSection;

public record VisualConfig(String name, String pluralName, String symbol) {

    public static VisualConfig fromConfig(ConfigurationSection section) {
        String name = section.getString("name");
        String pluralName = section.getString("pluralName");
        String symbol = section.getString("symbol");
        return new VisualConfig(name, pluralName, symbol);
    }

}
