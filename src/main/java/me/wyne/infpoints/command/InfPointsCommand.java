package me.wyne.infpoints.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.jorel.commandapi.CommandAPICommand;
import me.wyne.infpoints.InfPoints;
import me.wyne.infpoints.api.transaction.TransactionRequest.Operation;
import me.wyne.infpoints.point.PointManager;

@Singleton
public final class InfPointsCommand {

    private final InfPoints plugin;
    private final PointManager points;

    @Inject
    public InfPointsCommand(InfPoints plugin, PointManager points) {
        this.plugin = plugin;
        this.points = points;
        registerCommand();
    }

    private void registerCommand() {
        new CommandAPICommand("points")
                .withSubcommand(new ReloadCommand(plugin).command())
                .withSubcommand(new HelpCommand().command())
                .withSubcommand(new BalanceCommand(points).command())
                .withSubcommand(new PayCommand(points).command())
                .withSubcommand(new ModifyCommand(points, Operation.SET).command())
                .withSubcommand(new ModifyCommand(points, Operation.ADD).command())
                .withSubcommand(new ModifyCommand(points, Operation.SUBTRACT).command())
                .withSubcommand(new DeliverCommand(points).command())
                .withSubcommand(new ExchangeCommand(points).command())
                .withSubcommand(new HistoryCommand(points).command())
                .withSubcommand(new AuditCommand(points).command())
                .executes(HelpCommand::sendHelp)
                .register(plugin);
    }

}
