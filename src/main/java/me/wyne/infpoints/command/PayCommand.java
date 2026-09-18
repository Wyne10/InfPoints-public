package me.wyne.infpoints.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import me.wyne.infpoints.Messages;
import me.wyne.infpoints.api.Point;
import me.wyne.infpoints.api.PointProvider;
import me.wyne.infpoints.api.transaction.TransactionRequest;
import me.wyne.infpoints.api.transaction.TransactionResult.Status;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class PayCommand extends SubCommand {

    private final PointProvider points;

    public PayCommand(@NotNull PointProvider points) {
        super("pay");
        this.points = points;
    }

    @Override
    protected @NotNull CommandAPICommand build(@NotNull CommandAPICommand command) {
        return command
                .withArguments(CustomArguments.pointArgument("key", points), CustomArguments.targetsArgument("target"), new DoubleArgument("amount"))
                .executesPlayer((player, args) -> {
                    execute(player, CommandSupport.point(args), args);
                });
    }

    static void execute(@NotNull Player sender, @NotNull Point point, @NotNull CommandArguments args) throws WrapperCommandSyntaxException {
        CommandSupport.assertPermission(sender, "points.pay." + point.getKey());
        OfflinePlayer target = CommandSupport.resolveTarget(sender, args, "target");
        if (target.getUniqueId().equals(sender.getUniqueId()))
            throw CommandSupport.fail(sender, "error-pay-self", Messages.key(point));
        if (!point.supportsOfflinePlayers() && !target.isOnline())
            throw CommandSupport.fail(sender, "error-player-offline", Messages.key(point), Messages.playerName(target));
        double amount = (double) Objects.requireNonNull(args.get("amount"));
        TransactionRequest request = TransactionRequest.transfer(sender.getUniqueId(), target.getUniqueId(), amount)
                .withSource("command:pay")
                .withActor(sender.getUniqueId());
        CommandSupport.onMainThread(point.async().execute(request), result -> {
            if (result.status() == Status.INSUFFICIENT_FUNDS) {
                Messages.send(sender, "error-insufficient-funds", Messages.key(point));
                return;
            }
            if (!result.isApplied()) {
                CommandSupport.reportFailure(sender, point, result, target);
                return;
            }
            Messages.sendAbout(sender, target, "success-point-pay", Messages.key(point), Messages.amount(point, result.amount()), Messages.playerName(target));
            Player receiver = target.getPlayer();
            if (receiver != null)
                Messages.sendAbout(receiver, sender, "info-point-receive", Messages.key(point), Messages.amount(point, result.amount()), Messages.playerName(sender));
        });
    }

}
