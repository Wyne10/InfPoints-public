package me.wyne.infpoints.api.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Configuration of the per-point pay and balance commands.
 * <p>
 * {@code payCommand} and {@code balanceCommand} are {@code null} when not configured; use
 * {@link #usePayCommand()} and {@link #useBalanceCommand()} to check whether the corresponding command is
 * registered.
 */
public record CommandConfig(@Nullable String payCommand, @NotNull List<String> payAliases,
                            @Nullable String balanceCommand, @NotNull List<String> balanceAliases) {

    public CommandConfig {
        payAliases = List.copyOf(payAliases);
        balanceAliases = List.copyOf(balanceAliases);
    }

    /**
     * Returns whether a pay command is configured, i.e. {@link #payCommand()} is non-null and non-empty.
     */
    public boolean usePayCommand() {
        return payCommand != null && !payCommand.isEmpty();
    }

    /**
     * Returns whether a balance command is configured, i.e. {@link #balanceCommand()} is non-null and non-empty.
     */
    public boolean useBalanceCommand() {
        return balanceCommand != null && !balanceCommand.isEmpty();
    }

}
