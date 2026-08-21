package org.bigcraft.infpoints.api.config;

import org.bukkit.configuration.ConfigurationSection;

import java.text.DecimalFormat;

/**
 * Display configuration for a point type: how its balance is named and formatted
 * when shown to players.
 *
 * @param name          singular display name
 * @param pluralName    plural display name
 * @param symbol        currency symbol
 * @param color         display color, as used by the plugin's message formatting
 * @param decimalFormat format used to render balances, e.g. via {@link org.bigcraft.infpoints.api.Point#getFormat}
 */
public record VisualConfig(String name, String pluralName, String symbol, String color, DecimalFormat decimalFormat) {

    /**
     * Reads a {@code VisualConfig} from a configuration section, using an empty
     * string for any missing {@code name}, {@code namePlural}, {@code symbol} or
     * {@code color}, and {@code "#.##"} for a missing {@code decimalFormat}.
     */
    public static VisualConfig fromConfig(ConfigurationSection section) {
        String name = section.getString("name", "");
        String pluralName = section.getString("namePlural", "");
        String symbol = section.getString("symbol", "");
        String color = section.getString("color", "");
        String decimalFormat = section.getString("decimalFormat", "#.##");
        return new VisualConfig(name, pluralName, symbol, color, new DecimalFormat(decimalFormat));
    }

}
