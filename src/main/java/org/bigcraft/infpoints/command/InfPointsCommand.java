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
                .then(new StringArgument("point")
                        .replaceSuggestions(ArgumentSuggestions.stringCollection(info ->
                                pointManager.getPoints().keySet()))
                        .then(new LiteralArgument("balance")
                                .executesPlayer((sender, args) -> {
                                    hasPermission(sender, "points.balance." + getPointKey(sender, args));
                                    String point = getPointKey(sender, args);
                                    sender.sendMessage(I18n.global.getPlaceholderComponent(
                                            sender.locale(),
                                            sender,
                                        "info-point-balance",
                                            Placeholder.replace("key", point)
                                    ));
                                })
                                .then(new EntitySelectorArgument.OnePlayer("target")
                                        .executes((sender, args) -> {
                                            hasPermission(sender, "points.balance-other." + getPointKey(sender, args));
                                            Player player = args.getByClass("target", Player.class);
                                            String point = getPointKey(sender, args);
                                            sender.sendMessage(I18n.global.getPlaceholderComponent(
                                                    I18n.toLocale(sender),
                                                    player,
                                                    "info-point-balance-other",
                                                    Placeholder.replace("key", point)
                                            ));
                                })))
                        .then(new LiteralArgument("set")
                                .then(new IntegerArgument("amount")
                                        .then(new EntitySelectorArgument.OnePlayer("target")
                                                .executes((sender, args) -> {
                                                    addSetSub(sender, args, "points.set.", "success-point-set", PointType::set);
                                                }))))
                        .then(new LiteralArgument("add")
                                .then(new IntegerArgument("amount")
                                        .then(new EntitySelectorArgument.OnePlayer("target")
                                                .executes((sender, args) -> {
                                                    addSetSub(sender, args, "points.add.", "success-point-add", PointType::add);
                                                }))))
                        .then(new LiteralArgument("sub")
                                .then(new IntegerArgument("amount")
                                        .then(new EntitySelectorArgument.OnePlayer("target")
                                                .executes((sender, args) -> {
                                                    addSetSub(sender, args, "points.sub.", "success-point-sub", (point, player, amount) -> point.set(player, point.get(player) - amount));
                                                }))))
                        .then(new LiteralArgument("pay")
                                .then(new IntegerArgument("amount", 1)
                                        .then(new EntitySelectorArgument.OnePlayer("target")
                                                .executesPlayer((sender, args) -> {
                                                    hasPermission(sender, "points.pay." + getPointKey(sender, args));
                                                    Player player = args.getByClass("target", Player.class);
                                                    int amount = args.getByClassOrDefault("amount", Integer.class, 1);
                                                    Point point = getPoint(sender, args);
                                                    boolean result = point.transfer(sender.getUniqueId(), player.getUniqueId(), amount);
                                                    if (!result)
                                                        sender.sendMessage(I18n.global.getPlaceholderComponent(
                                                                sender.locale(),
                                                                sender,
                                                                "error-insufficient-balance",
                                                                Placeholder.replace("key", args.getRaw("point"))
                                                        ));
                                                    else
                                                        sender.sendMessage(I18n.global.getPlaceholderComponent(
                                                                sender.locale(),
                                                                sender,
                                                                "success-point-pay",
                                                                Placeholder.replace("key", args.getRaw("point")),
                                                                Placeholder.replace("amount", amount),
                                                                Placeholder.replace("player-name", player.getName())
                                                        ));
                                        })))))
                .then(new LiteralArgument("reload")
                        .withPermission("points.admin.reload")
                        .executes((sender, args) -> {
                            plugin.reload();
                            sender.sendMessage(I18n.global.getPlaceholderComponent(I18n.toLocale(sender), sender, "success-plugin-reload"));
                        }))
                .register();
    }

    private void addSetSub(CommandSender sender, CommandArguments args, String permission, String message, AddSetSub operation) throws WrapperCommandSyntaxException {
        hasPermission(sender, permission + getPointKey(sender, args));
        Player player = args.getByClass("target", Player.class);
        int amount = args.getByClassOrDefault("amount", Integer.class, 0);
        Point point = getPoint(sender, args);
        operation.execute(point, player.getUniqueId(), amount);
        sender.sendMessage(I18n.global.getPlaceholderComponent(
                I18n.toLocale(sender),
                player,
                message,
                Placeholder.replace("key", args.getRaw("point")),
                Placeholder.replace("amount", amount),
                Placeholder.replace("player-name", player.getName())
        ));
    }

    @FunctionalInterface
    interface AddSetSub {
        void execute(Point point, UUID player, int amount);
    }

    private void hasPermission(CommandSender sender, String permission) throws WrapperCommandSyntaxException{
        if (!sender.hasPermission(permission))
            throw CommandAPIBukkit.failWithAdventureComponent(I18n.global.getPlaceholderComponent(
                    I18n.toLocale(sender),
                    sender,
                    "error-permissions"
            ));
    }

    private String getPointKey(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
        String point = args.getOrDefaultRaw("point", "");

        if (!pointManager.getPoints().containsKey(point))
            throw CommandAPIBukkit.failWithAdventureComponent(I18n.global.getPlaceholderComponent(
                    I18n.toLocale(sender),
                    sender,
                    "error-point-not-found",
                    Placeholder.replace("key", point)
            ));

        return point;
    }

    private Point getPoint(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
        String point = args.getOrDefaultRaw("point", "");

        if (!pointManager.getPoints().containsKey(point))
            throw CommandAPIBukkit.failWithAdventureComponent(I18n.global.getPlaceholderComponent(
                    I18n.toLocale(sender),
                    sender,
                    "error-point-not-found",
                    Placeholder.replace("key", point)
            ));

        return pointManager.getPoints().get(point);
    }

}
