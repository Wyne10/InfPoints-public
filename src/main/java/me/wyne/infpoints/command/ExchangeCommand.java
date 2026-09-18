package me.wyne.infpoints.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import me.wyne.wutils.common.command.CommandUtils;
import me.wyne.wutils.i18n.I18n;
import me.wyne.infpoints.InfPoints;
import me.wyne.infpoints.Messages;
import me.wyne.infpoints.api.Point;
import me.wyne.infpoints.api.PointProvider;
import me.wyne.infpoints.api.transaction.TransactionRequest;
import me.wyne.infpoints.api.transaction.TransactionResult.Status;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class ExchangeCommand extends SubCommand {

    private final PointProvider points;

    public ExchangeCommand(@NotNull PointProvider points) {
        super("exchange");
        this.points = points;
    }

    @Override
    protected @NotNull CommandAPICommand build(@NotNull CommandAPICommand command) {
        return command
                .withArguments(CustomArguments.pointArgument("key", points), CommandUtils.onlinePlayer("target"), new DoubleArgument("amount"),
                        new GreedyStringArgument("execute"))
                .executes(this::execute);
    }

    private void execute(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
        Point point = CommandSupport.point(args);
        CommandSupport.assertPermission(sender, "points.exchange." + point.getKey());
        Player target = (Player) Objects.requireNonNull(args.get("target"));
        double amount = (double) Objects.requireNonNull(args.get("amount"));
        String command = Objects.requireNonNullElse(args.getByClass("execute", String.class), "");
        TransactionRequest request = TransactionRequest.subtract(target.getUniqueId(), amount)
                .withReason("Exchange: " + command)
                .withSource("command:exchange")
                .withActor(CommandSupport.actor(sender));
        CommandSupport.onMainThread(point.async().execute(request), result -> {
            if (result.status() == Status.INSUFFICIENT_FUNDS) {
                Messages.send(target, "error-insufficient-funds", Messages.key(point));
                return;
            }
            if (!result.isApplied()) {
                CommandSupport.reportFailure(sender, point, result, target);
                return;
            }
            Messages.send(target, "info-balance-sub", Messages.key(point), Messages.amount(point, result.amount()));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), I18n.global.accessor(target, command).getPlaceholderString(target).get());
            InfPoints.logger().info("{} exchanged {} of '{}' for '{}'", target.getName(), Messages.format(point, result.amount()), point.getKey(), command);
        });
    }

}
