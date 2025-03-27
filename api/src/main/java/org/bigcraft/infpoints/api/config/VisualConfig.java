package org.bigcraft.infpoints.api.config;

import org.bukkit.configuration.ConfigurationSection;

import java.text.DecimalFormat;

public record VisualConfig(String name, String pluralName, String symbol, String color, DecimalFormat decimalFormat) {

    public static VisualConfig fromConfig(ConfigurationSection section) {
        String name = section.getString("name", "");
        String pluralName = section.getString("namePlural", "");
        String symbol = section.getString("symbol", "");
        String color = section.getString("color", "");
        String decimalFormat = section.getString("decimalFormat", "#.##");
        return new VisualConfig(name, pluralName, symbol, color, new DecimalFormat(decimalFormat));
    }

}
