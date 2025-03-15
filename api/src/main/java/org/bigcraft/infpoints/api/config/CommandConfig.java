package org.bigcraft.infpoints.api.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public record CommandConfig(String payCommand, List<String> payAliases, String balanceCommand, List<String> balanceAliases) {

    public static CommandConfig fromConfig(ConfigurationSection section) {
        String payCommand = section.getString("payCommand");
        List<String> payAliases = section.getStringList("payAliases");
        String balanceCommand = section.getString("balanceCommand");
        List<String> balanceAliases = section.getStringList("balanceAliases");
        return new CommandConfig(payCommand, payAliases, balanceCommand, balanceAliases);
    }

}
