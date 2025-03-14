package org.bigcraft.infpoints.config;

import org.bukkit.configuration.ConfigurationSection;

public record PointConfig(String type, boolean canPay, boolean canCheckBalance) {

    public static PointConfig fromConfig(ConfigurationSection section) {
        String type = section.getString("type");
        boolean canPay = section.getBoolean("canPay");
        boolean canCheckBalance = section.getBoolean("canCheckBalance");
        return new PointConfig(type, canPay, canCheckBalance);
    }

}
