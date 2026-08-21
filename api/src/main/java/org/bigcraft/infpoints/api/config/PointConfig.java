package org.bigcraft.infpoints.api.config;

import org.bigcraft.infpoints.api.PointTypes;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Core configuration for a single point type.
 *
 * @param key            the point's configuration key, also used to look it up via
 *                       {@link org.bigcraft.infpoints.api.PointProvider}
 * @param type           the raw storage type name as written in the config, equal
 *                       to {@code pointType.name()}
 * @param pointType      the parsed storage backend
 * @param defaultBalance the balance a player has before any balance has been set for them
 */
public record PointConfig(String key, String type, PointTypes pointType, double defaultBalance) {

    /**
     * Reads a {@code PointConfig} from a configuration section, using the section's
     * name as the key.
     *
     * @throws IllegalArgumentException if {@code type} is missing or is not a valid
     *                                  {@link PointTypes} name
     */
    public static PointConfig fromConfig(ConfigurationSection section) {
        String key = section.getName();
        String type = section.getString("type");
        PointTypes pointType = PointTypes.valueOf(type);
        double defaultBalance = section.getLong("defaultBalance");
        return new PointConfig(key, type, pointType, defaultBalance);
    }

}
