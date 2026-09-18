package org.bigcraft.infpoints.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import me.wyne.wutils.i18n.language.replacement.Placeholder;
import org.bigcraft.infpoints.InfPoints;
import org.bigcraft.infpoints.Messages;
import org.bigcraft.infpoints.api.Point;
import org.bigcraft.infpoints.api.PointProvider;
import org.bigcraft.infpoints.api.transaction.TransactionRequest;
import org.bigcraft.infpoints.api.transaction.TransactionRequest.Operation;
import org.bigcraft.infpoints.api.transaction.TransactionResult;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

// set, add and sub for one player or many at once, e.g. /points add primary @a 10
public final class ModifyCommand extends SubCommand {

    private final PointProvider points;
    private final Operation operation;
    private final String action;

    public ModifyCommand(@NotNull PointProvider points, @NotNull Operation operation) {
        super(action(operation));
        this.points = points;
        this.operation = operation;
        this.action = action(operation);
    }

    @Override
    protected @NotNull CommandAPICommand build(@NotNull CommandAPICommand command) {
        return command
                .withArguments(CustomArguments.pointArgument("key", points), CustomArguments.targetsArgument("targets"), new DoubleArgument("amount"))
                .withOptionalArguments(new BooleanArgument("sender-message"), new BooleanArgument("receiver-message"), new GreedyStringArgument("reason"))
                .executes(this::execute);
    }

    private void execute(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
        Point point = CommandSupport.point(args);
        CommandSupport.assertPermission(sender, "points." + action + "." + point.getKey());
        List<OfflinePlayer> targets = CommandSupport.resolveTargets(sender, args, "targets");
        double amount = (double) Objects.requireNonNull(args.get("amount"));
        boolean senderMessage = args.getByClassOrDefault("sender-message", Boolean.class, true);
        boolean receiverMessage = args.getByClassOrDefault("receiver-message", Boolean.class, true);
        String reason = args.getByClass("reason", String.class);
        UUID actor = CommandSupport.actor(sender);

        List<CompletableFuture<TransactionResult>> results = new ArrayList<>();
        for (OfflinePlayer target : targets) {
            TransactionRequest request = request(target.getUniqueId(), amount)
                    .withReason(reason)
                    .withSource("command:" + action)
                    .withActor(actor);
            results.add(point.async().execute(request));
        }
        CommandSupport.onMainThread(CompletableFuture.allOf(results.toArray(CompletableFuture[]::new)),
                ignored -> report(sender, point, targets, results, amount, senderMessage, receiverMessage));
    }

    private void report(CommandSender sender, Point point, List<OfflinePlayer> targets, List<CompletableFuture<TransactionResult>> results,
                        double amount, boolean senderMessage, boolean receiverMessage) {
        int applied = 0;
        double appliedAmount = amount;
        for (int i = 0; i < targets.size(); i++) {
            OfflinePlayer target = targets.get(i);
            TransactionResult result = results.get(i).join();
            if (!result.isApplied()) {
                if (targets.size() == 1)
                    CommandSupport.reportFailure(sender, point, result, target);
                continue;
            }
            applied++;
            appliedAmount = result.amount();
            Player receiver = target.getPlayer();
            if (receiverMessage && receiver != null)
                Messages.send(receiver, "info-balance-" + action, Messages.key(point), Messages.amount(point, result.amount()));
            if (senderMessage && targets.size() == 1)
                Messages.sendAbout(sender, target, "success-point-" + action, Messages.key(point), Messages.amount(point, result.amount()), Messages.playerName(target));
        }
        if (targets.size() > 1) {
            if (senderMessage)
                Messages.send(sender, "success-point-" + action + "-many", Messages.key(point), Messages.amount(point, appliedAmount), Placeholder.replace("count", applied));
            if (applied < targets.size())
                Messages.send(sender, "error-targets-failed", Messages.key(point), Placeholder.replace("count", targets.size() - applied));
        }
        InfPoints.logger().info("{} ran /points {} {} {} for {} of {} players", sender.getName(), action, point.getKey(),
                Messages.format(point, appliedAmount), applied, targets.size());
    }

    private TransactionRequest request(UUID player, double amount) {
        return switch (operation) {
            case SET -> TransactionRequest.set(player, amount);
            case ADD -> TransactionRequest.add(player, amount);
            case SUBTRACT -> TransactionRequest.subtract(player, amount);
            case TRANSFER -> throw new IllegalStateException("Transfers are run by the pay command");
        };
    }

    private static String action(Operation operation) {
        return switch (operation) {
            case SET -> "set";
            case ADD -> "add";
            case SUBTRACT -> "sub";
            case TRANSFER -> throw new IllegalArgumentException("Transfers are run by the pay command");
        };
    }

}
