package org.bigcraft.infpoints.command;

import dev.jorel.commandapi.CommandAPIBukkit;
import dev.jorel.commandapi.CommandAPICommand;
import org.bigcraft.infpoints.core.Point;
import org.bukkit.Bukkit;

public class BalanceCommand extends PersonalCommand {

    public BalanceCommand(Point point) {
        super(point);
    }

    @Override
    public void register() {
        new CommandAPICommand(getPoint().getCommandConfig().balanceCommand())
                .withAliases(getPoint().getCommandConfig().balanceAliases().toArray(String[]::new))
                .executesPlayer((sender, args) -> {
                    StringBuilder commandBuilder = new StringBuilder();
                    commandBuilder.append("points ")
                            .append(getPoint().getConfig().key())
                            .append(" balance");
                    Bukkit.dispatchCommand(sender, commandBuilder.toString());
                })
                .register();
    }

    @Override
    public void unregister() {
        CommandAPIBukkit.unregister(getPoint().getCommandConfig().balanceCommand(), true, true);
    }

}
