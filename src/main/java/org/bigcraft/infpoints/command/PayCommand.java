package org.bigcraft.infpoints.command;

import dev.jorel.commandapi.CommandAPIBukkit;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.IntegerArgument;
import me.wyne.wutils.common.command.CommandUtils;
import org.bigcraft.infpoints.InfPoints;
import org.bigcraft.infpoints.core.Point;
import org.bukkit.Bukkit;

public class PayCommand extends PersonalCommand {

    public PayCommand(Point point) {
        super(point);
    }

    @Override
    public void register() {
        new CommandTree(getPoint().getCommandConfig().payCommand())
                .executes(InfPointsCommand::sendHelp)
                .withAliases(getPoint().getCommandConfig().payAliases().toArray(String[]::new))
                .then(CommandUtils.onlinePlayer("target").executes(InfPointsCommand::sendHelp)
                        .then(new IntegerArgument("amount", 1)
                                .executes((sender, args) -> {
                                    StringBuilder commandBuilder = new StringBuilder();
                                    commandBuilder.append("points ")
                                            .append("pay ")
                                            .append(getPoint().getConfig().key())
                                            .append(" ")
                                            .append(args.getRaw("target"))
                                            .append(" ")
                                            .append(args.getRaw("amount"));
                                    Bukkit.dispatchCommand(sender, commandBuilder.toString());
                                })))
                .register(InfPoints.getInstance());
    }

    @Override
    public void unregister() {
        CommandAPIBukkit.unregister(getPoint().getCommandConfig().payCommand(), true, true);
    }

}
