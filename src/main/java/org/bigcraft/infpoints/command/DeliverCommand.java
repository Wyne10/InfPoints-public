package org.bigcraft.infpoints.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import me.wyne.wutils.i18n.language.replacement.Placeholder;
import org.bigcraft.infpoints.InfPoints;
import org.bigcraft.infpoints.Messages;
import org.bigcraft.infpoints.api.Point;
import org.bigcraft.infpoints.api.PointProvider;
import org.bigcraft.infpoints.api.transaction.TransactionRequest;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

// Adds points at most once per delivery id, so a web store resending its command can't pay out twice
public final class DeliverCommand extends SubCommand {

    private final PointProvider points;

    public DeliverCommand(@NotNull PointProvider points) {
        super("deliver");
        this.points = points;
    }

    @Override
    protected @NotNull CommandAPICommand build(@NotNull CommandAPICommand command) {
        return command
                .withArguments(CustomArguments.pointArgument("key", points), CustomArguments.targetsArgument("target"), new DoubleArgument("amount"),
                        new TextArgument("id"))
                .withOptionalArguments(new GreedyStringArgument("reason"))
                .executes(this::execute);
    }

    private void execute(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
        Point point = CommandSupport.point(args);
        CommandSupport.assertPermission(sender, "points.add." + point.getKey());
        if (!point.supportsHistory())
            throw CommandSupport.fail(sender, "error-history-unsupported", Messages.key(point));
        OfflinePlayer target = CommandSupport.resolveTarget(sender, args, "target");
        String id = Objects.requireNonNullElse(args.getByClass("id", String.class), "");
        if (id.isEmpty() || id.length() > TransactionRequest.MAX_IDEMPOTENCY_KEY_LENGTH)
            throw CommandSupport.fail(sender, "error-invalid-delivery-id", Placeholder.replace("max", TransactionRequest.MAX_IDEMPOTENCY_KEY_LENGTH));
        double amount = (double) Objects.requireNonNull(args.get("amount"));
        String reason = args.getByClass("reason", String.class);
        TransactionRequest request = TransactionRequest.add(target.getUniqueId(), amount)
                .withIdempotencyKey(id)
                .withReason(reason != null ? reason : "Delivery " + id)
                .withSource("command:deliver")
                .withActor(CommandSupport.actor(sender));
        CommandSupport.onMainThread(point.async().execute(request), result -> {
            switch (result.status()) {
                case SUCCESS -> {
                    Messages.send(sender, "success-point-deliver", Messages.key(point), Messages.amount(point, result.amount()),
                            Messages.playerName(target), Placeholder.replace("id", id));
                    Player receiver = target.getPlayer();
                    if (receiver != null)
                        Messages.send(receiver, "info-balance-add", Messages.key(point), Messages.amount(point, result.amount()));
                    InfPoints.logger().info("{} delivered {} of '{}' to {} with id '{}'", sender.getName(), Messages.format(point, result.amount()),
                            point.getKey(), Messages.name(target), id);
                }
                case DUPLICATE -> Messages.send(sender, "info-delivery-duplicate", Messages.key(point), Messages.playerName(target), Placeholder.replace("id", id));
                default -> CommandSupport.reportFailure(sender, point, result, target);
            }
        });
    }

}
