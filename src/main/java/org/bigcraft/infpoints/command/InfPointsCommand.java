package org.bigcraft.infpoints.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.jorel.commandapi.CommandAPIBukkit;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import me.wyne.wutils.i18n.I18n;
import me.wyne.wutils.i18n.language.replacement.Placeholder;
import org.bigcraft.infpoints.InfPoints;
import org.bigcraft.infpoints.api.PointType;
import org.bigcraft.infpoints.core.Point;
import org.bigcraft.infpoints.core.PointManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

@Singleton
public class InfPointsCommand {

    private final InfPoints plugin;
    private final PointManager pointManager;

    @Inject
    public InfPointsCommand(InfPoints plugin, PointManager pointManager) {
        this.plugin = plugin;
        this.pointManager = pointManager;
        registerCommand();
    }

    public void registerCommand() {
        new CommandTree("points")
                .executes(InfPointsCommand::sendHelp)
                .then(new LiteralArgument("help").executes(InfPointsCommand::sendHelp))
                .then(new StringArgument("point")
                        .replaceSuggestions(ArgumentSuggestions.stringCollection(info ->
                                pointManager.getPoints().keySet()))
                        .executes(InfPointsCommand::sendHelp)
                        .then(new LiteralArgument("balance")
                                .executesPlayer((sender, args) -> {
                                    hasPermission(sender, "points.balance." + getPointKey(sender, args));
                                    String point = getPointKey(sender, args);
                                    I18n.global.getPlaceholderComponent(
                                            sender.locale(),
                                            sender,
                                        "info-point-balance",
                                            Placeholder.replace("key", point)
                                    ).sendMessage(sender);
                                })
                                .then(new EntitySelectorArgument.OnePlayer("target")
                                        .executes((sender, args) -> {
                                            hasPermission(sender, "points.balance-other." + getPointKey(sender, args));
                                            Player player = args.getByClass("target", Player.class);
                                            String point = getPointKey(sender, args);
                                            I18n.global.getPlaceholderComponent(
                                                    I18n.toLocale(sender),
                                                    player,
                                                    "info-point-balance-other",
                                                    Placeholder.replace("key", point)
                                            ).sendMessage(sender);
                                })))
                        .then(new LiteralArgument("set").executes(InfPointsCommand::sendHelp)
                                .then(new DoubleArgument("amount").executes(InfPointsCommand::sendHelp)
                                        .then(new EntitySelectorArgument.OnePlayer("target")
                                                .executes((sender, args) -> {
                                                    addSetSub(sender, args, "points.set.", "success-point-set", "info-balance-set", PointType::set);
                                                }))))
                        .then(new LiteralArgument("add").executes(InfPointsCommand::sendHelp)
                                .then(new DoubleArgument("amount").executes(InfPointsCommand::sendHelp)
                                        .then(new EntitySelectorArgument.OnePlayer("target")
                                                .executes((sender, args) -> {
                                                    addSetSub(sender, args, "points.add.", "success-point-add", "info-balance-add", PointType::add);
                                                }))))
                        .then(new LiteralArgument("sub").executes(InfPointsCommand::sendHelp)
                                .then(new DoubleArgument("amount").executes(InfPointsCommand::sendHelp)
                                        .then(new EntitySelectorArgument.OnePlayer("target")
                                                .executes((sender, args) -> {
                                                    addSetSub(sender, args, "points.sub.", "success-point-sub", "info-balance-sub", PointType::subtract);
                                                }))))
                        .then(new LiteralArgument("pay").executes(InfPointsCommand::sendHelp)
                                .then(new IntegerArgument("amount", 1).executes(InfPointsCommand::sendHelp)
                                        .then(new EntitySelectorArgument.OnePlayer("target")
                                                .executesPlayer((sender, args) -> {
                                                    hasPermission(sender, "points.pay." + getPointKey(sender, args));
                                                    Player player = args.getByClass("target", Player.class);
                                                    if (player == sender)
                                                        throw CommandAPIBukkit.failWithBaseComponents(I18n.global.getPlaceholderComponent(
                                                                sender.locale(),
                                                                sender,
                                                                "error-pay-self",
                                                                Placeholder.replace("key", args.getRaw("point"))
                                                        ).bungee());

                                                    int amount = args.getByClassOrDefault("amount", Integer.class, 1);
                                                    Point point = getPoint(sender, args);
                                                    boolean result = point.transfer(sender.getUniqueId(), player.getUniqueId(), amount);
                                                    if (!result)
                                                        I18n.global.getPlaceholderComponent(
                                                                sender.locale(),
                                                                sender,
                                                                "error-insufficient-funds",
                                                                Placeholder.replace("key", args.getRaw("point"))
                                                        ).sendMessage(sender);
                                                    else
                                                        I18n.global.getPlaceholderComponent(
                                                                sender.locale(),
                                                                sender,
                                                                "success-point-pay",
                                                                Placeholder.replace("key", args.getRaw("point")),
                                                                Placeholder.replace("amount", point.getVisualConfig().decimalFormat().format(amount)),
                                                                Placeholder.replace("player-name", player.getName())
                                                        ).sendMessage(sender);
                                        })))))
                .then(new LiteralArgument("reload")
                        .withPermission("points.admin.reload")
                        .executes((sender, args) -> {
                            plugin.reload();
                            I18n.global.getPlaceholderComponent(I18n.toLocale(sender), sender, "success-plugin-reload")
                                    .sendMessage(sender);
                        }))
                .register(plugin);
    }

    private void addSetSub(CommandSender sender, CommandArguments args, String permission, String message, String receiverMessage, AddSetSub operation) throws WrapperCommandSyntaxException {
        hasPermission(sender, permission + getPointKey(sender, args));
        Player player = args.getByClass("target", Player.class);
        double amount = args.getByClassOrDefault("amount", Double.class, 0D);
        Point point = getPoint(sender, args);
        operation.execute(point, player.getUniqueId(), amount);
        I18n.global.getPlaceholderComponent(
                I18n.toLocale(sender),
                player,
                message,
                Placeholder.replace("key", args.getRaw("point")),
                Placeholder.replace("amount", point.getVisualConfig().decimalFormat().format(amount)),
                Placeholder.replace("player-name", player.getName())
        ).sendMessage(sender);
        I18n.global.getPlaceholderComponent(player.locale(), player, receiverMessage,
                Placeholder.replace("key", args.getRaw("point")),
                Placeholder.replace("amount", point.getVisualConfig().decimalFormat().format(amount))
                ).sendMessage(player);
    }

    @FunctionalInterface
    interface AddSetSub {
        void execute(Point point, UUID player, double amount);
    }

    private void hasPermission(CommandSender sender, String permission) throws WrapperCommandSyntaxException{
        if (!sender.hasPermission(permission))
            throw CommandAPIBukkit.failWithBaseComponents(I18n.global.getPlaceholderComponent(
                    I18n.toLocale(sender),
                    sender,
                    "error-permissions"
            ).bungee());
    }

    private String getPointKey(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
        String point = args.getOrDefaultRaw("point", "");

        if (!pointManager.getPoints().containsKey(point))
            throw CommandAPIBukkit.failWithBaseComponents(I18n.global.getPlaceholderComponent(
                    I18n.toLocale(sender),
                    sender,
                    "error-point-not-found",
                    Placeholder.replace("key", point)
            ).bungee());

        return point;
    }

    private Point getPoint(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
        String point = args.getOrDefaultRaw("point", "");

        if (!pointManager.getPoints().containsKey(point))
            throw CommandAPIBukkit.failWithBaseComponents(I18n.global.getPlaceholderComponent(
                    I18n.toLocale(sender),
                    sender,
                    "error-point-not-found",
                    Placeholder.replace("key", point)
            ).bungee());

        return pointManager.getPoints().get(point);
    }
    
    public static void sendHelp(CommandSender sender, CommandArguments args) {
        sender.sendMessage(I18n.global.getPlaceholderComponent(I18n.toLocale(sender), sender, "info-help"));
    }

}
