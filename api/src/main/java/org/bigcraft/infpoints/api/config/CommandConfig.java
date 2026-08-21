package org.bigcraft.infpoints.api.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

/**
 * Configuration for the per-point-type pay and balance commands.
 * <p>
 * {@code payCommand} and {@code balanceCommand} are {@code null} when not
 * configured; use {@link #usePayCommand()} and {@link #useBalanceCommand()}
 * to check whether the corresponding command should be registered.
 */
public record CommandConfig(String payCommand, List<String> payAliases, String balanceCommand, List<String> balanceAliases) {

    /**
     * Reads a {@code CommandConfig} from a configuration section. Command names
     * are {@code null} if the corresponding key is absent; alias lists default
     * to empty.
     */
    public static CommandConfig fromConfig(ConfigurationSection section) {
        String payCommand = section.getString("payCommand");
        List<String> payAliases = section.getStringList("payAliases");
        String balanceCommand = section.getString("balanceCommand");
        List<String> balanceAliases = section.getStringList("balanceAliases");
        return new CommandConfig(payCommand, payAliases, balanceCommand, balanceAliases);
    }

    /**
     * Returns whether a pay command is configured, i.e. {@link #payCommand()} is
     * non-null and non-empty.
     */
    public boolean usePayCommand() {
        return payCommand != null && !payCommand.isEmpty();
    }

    /**
     * Returns whether a balance command is configured, i.e. {@link #balanceCommand()}
     * is non-null and non-empty.
     */
    public boolean useBalanceCommand() {
        return balanceCommand != null && !balanceCommand.isEmpty();
    }

}
