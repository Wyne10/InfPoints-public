package org.bigcraft.infpoints.command;

import dev.jorel.commandapi.CommandAPIBukkit;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
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
                .then(new IntegerArgument("amount", 1).executes(InfPointsCommand::sendHelp)
                        .then(new EntitySelectorArgument.OnePlayer("target")
                                .executes((sender, args) -> {
                                    StringBuilder commandBuilder = new StringBuilder();
                                    commandBuilder.append("points ")
                                            .append(getPoint().getConfig().key())
                                            .append(" pay ")
                                            .append(args.getRaw("amount"))
                                            .append(" ")
                                            .append(args.getRaw("target"));
                                    Bukkit.dispatchCommand(sender, commandBuilder.toString());
                                })))
                .register();
    }

    @Override
    public void unregister() {
        CommandAPIBukkit.unregister(getPoint().getCommandConfig().payCommand(), true, true);
    }

}
