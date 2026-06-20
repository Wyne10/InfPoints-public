package org.bigcraft.infpoints.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.jorel.commandapi.CommandAPIBukkit;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import me.wyne.wutils.common.command.CommandUtils;
import me.wyne.wutils.i18n.I18n;
import me.wyne.wutils.i18n.language.replacement.Placeholder;
import org.bigcraft.infpoints.InfPoints;
import org.bigcraft.infpoints.api.PointType;
import org.bigcraft.infpoints.core.Point;
import org.bigcraft.infpoints.core.PointManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
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
        new CommandAPICommand("points")
                .withSubcommand(reloadCommand())
                .withSubcommand(helpCommand())
                .withSubcommand(balanceCommand())
                .withSubcommand(payCommand())
                .withSubcommand(crudCommand("set", "points.set", "success-point-set", "info-balance-set", PointType::set))
                .withSubcommand(crudCommand("add", "points.add", "success-point-add", "info-balance-add", PointType::add))
                .withSubcommand(crudCommand("sub", "points.sub", "success-point-sub", "info-balance-sub", PointType::subtract))
                .withSubcommand(multiCrudCommand("set-many", "points.set", "success-point-set", "info-balance-set", PointType::set))
                .withSubcommand(multiCrudCommand("add-many", "points.add", "success-point-add", "info-balance-add", PointType::add))
                .withSubcommand(multiCrudCommand("sub-many", "points.sub", "success-point-sub", "info-balance-sub", PointType::subtract))
                .withSubcommand(exchangeCommand())
                .executes(InfPointsCommand::sendHelp)
                .register(plugin);
    }

    private CommandAPICommand reloadCommand() {
        return new CommandAPICommand("reload")
                .withPermission("points.admin.reload")
                .executes((sender, args) -> {
                    plugin.reload();
                    I18n.global.accessor(sender, "success-plugin-reload").getPlaceholderComponent(sender)
                            .sendMessage(sender);
                });
    }

    private static CommandAPICommand helpCommand() {
        return new CommandAPICommand("help")
                .executes(InfPointsCommand::sendHelp);
    }

    public static void sendHelp(CommandSender sender, CommandArguments args) {
        I18n.global.accessor(sender, "info-help").getPlaceholderComponent(sender).sendMessage(sender);
    }

    private CommandAPICommand balanceCommand() {
        return new CommandAPICommand("balance")
                .withArguments(pointKeyArgument("key"))
                .withOptionalArguments(CommandUtils.offlinePlayer("target"))
                .executes((sender, args) -> {
                    var key = args.getOrDefaultRaw("key", "");
                    assertPointKeyExists(key, sender);
                    var targetName = args.getOptionalByClass("target", String.class)
                            .orElse(sender.getName());
                    var target = CommandUtils.getOfflinePlayer(args, "target");
                    if (!targetName.equals(sender.getName())) {
                        assertHasPermission(sender, "points.balance-other." + key);
                        sendBalanceOther(sender, targetName, target, key);
                    }
                    else {
                        assertHasPermission(sender, "points.balance." + key);
                        if (!(sender instanceof Player player)) return;
                        sendBalance(player, key);
                    }
                });
    }

    private void sendBalance(Player sender, String pointKey) {
        I18n.global.accessor(sender, "info-point-balance").getPlaceholderComponent(sender,
                Placeholder.replace("key", pointKey)
        ).sendMessage(sender);
    }

    private void sendBalanceOther(CommandSender sender, String targetName, @Nullable OfflinePlayer target, String pointKey) throws WrapperCommandSyntaxException {
        if (target == null)
            throw CommandAPIBukkit.failWithBaseComponents(
                    I18n.global.accessor(sender, "error-player-not-found")
                            .getPlaceholderComponent(sender, Placeholder.replace("name", targetName)).bungee()
            );
        I18n.global.accessor(sender, "info-point-balance-other").getPlaceholderComponent(target,
                Placeholder.replace("key", pointKey)
        ).sendMessage(sender);
    }

    private CommandAPICommand payCommand() {
        return new CommandAPICommand("pay")
                .withArguments(pointKeyArgument("key"))
                .withArguments(CommandUtils.onlinePlayer("target"))
                .withArguments(new IntegerArgument("amount", 1))
                .executesPlayer((sender, args) -> {
                    var key = args.getOrDefaultRaw("key", "");
                    assertPointKeyExists(key, sender);
                    assertHasPermission(sender, "points.pay." + key);
                    var point = pointManager.getPoints().get(key);
                    var target = args.getByClass("target", Player.class);
                    if (target == sender)
                        throw CommandAPIBukkit.failWithBaseComponents(
                                I18n.global.accessor(sender, "error-pay-self")
                                        .getPlaceholderComponent(sender, Placeholder.replace("key", key)).bungee()
                        );

                    var amount = args.getByClassOrDefault("amount", Integer.class, 1);
                    var result = point.transfer(sender.getUniqueId(), target.getUniqueId(), amount);
                    if (!result)
                        throw CommandAPIBukkit.failWithBaseComponents(
                                I18n.global.accessor(sender, "error-insufficient-funds")
                                        .getPlaceholderComponent(sender, Placeholder.replace("key", key)).bungee()
                        );

                    I18n.global.accessor(sender, "success-point-pay").getPlaceholderComponent(sender,
                            Placeholder.replace("key", key),
                            Placeholder.replace("amount", point.getVisualConfig().decimalFormat().format(amount)),
                            Placeholder.replace("player-name", target.getName())
                    ).sendMessage(sender);
                    I18n.global.accessor(target, "info-point-receive").getPlaceholderComponent(sender,
                            Placeholder.replace("key", key),
                            Placeholder.replace("amount", point.getVisualConfig().decimalFormat().format(amount))
                    ).sendMessage(target);
                });
    }

    private CommandAPICommand crudCommand(String commandName, String permission, String message, String receiverMessage, AddSetSub operation) {
        return new CommandAPICommand(commandName)
                .withArguments(pointKeyArgument("key"))
                .withArguments(CommandUtils.offlinePlayer("target"))
                .withArguments(new DoubleArgument("amount"))
                .withOptionalArguments(new BooleanArgument("sender-message"))
                .withOptionalArguments(new BooleanArgument("receiver-message"))
                .executes((sender, args) -> {
                    var key = args.getOrDefaultRaw("key", "");
                    assertPointKeyExists(key, sender);
                    assertHasPermission(sender, permission + "." + key);
                    var targetName = args.getOrDefaultRaw("target", "");
                    var target = CommandUtils.getOfflinePlayer(args, "target");
                    if (target == null)
                        throw CommandAPIBukkit.failWithBaseComponents(
                                I18n.global.accessor(sender, "error-player-not-found")
                                        .getPlaceholderComponent(sender, Placeholder.replace("name", targetName)).bungee()
                        );

                    var amount = args.getByClassOrDefault("amount", Double.class, 0D);
                    var point = pointManager.getPoints().get(key);
                    operation.execute(point, target.getUniqueId(), amount);

                    if (args.getByClassOrDefault("sender-message", Boolean.class, true))
                        I18n.global.accessor(sender, message).getPlaceholderComponent(target,
                                Placeholder.replace("key", key),
                                Placeholder.replace("amount", point.getVisualConfig().decimalFormat().format(amount)),
                                Placeholder.replace("player-name", target.getName())
                        ).sendMessage(sender);

                    if (args.getByClassOrDefault("receiver-message", Boolean.class, true) && target.isOnline())
                        I18n.global.accessor(target, receiverMessage).getPlaceholderComponent(target,
                                Placeholder.replace("key", key),
                                Placeholder.replace("amount", point.getVisualConfig().decimalFormat().format(amount))
                        ).sendMessage(target.getPlayer());
                });
    }

    private CommandAPICommand multiCrudCommand(String commandName, String permission, String message, String receiverMessage, AddSetSub operation) {
        return new CommandAPICommand(commandName)
                .withArguments(pointKeyArgument("key"))
                .withArguments(new EntitySelectorArgument.ManyPlayers("targets"))
                .withArguments(new DoubleArgument("amount"))
                .withOptionalArguments(new BooleanArgument("sender-message"))
                .withOptionalArguments(new BooleanArgument("receiver-message"))
                .executes((sender, args) -> {
                    var key = args.getOrDefaultRaw("key", "");
                    assertPointKeyExists(key, sender);
                    assertHasPermission(sender, permission + "." + key);
                    @SuppressWarnings("unchecked")
                    var targets = (Collection<Player>) args.get("targets");
                    if (targets == null || targets.isEmpty())
                        throw CommandAPIBukkit.failWithBaseComponents(
                                I18n.global.accessor(sender, "error-player-not-found")
                                        .getPlaceholderComponent(sender, Placeholder.replace("name", args.getOrDefaultRaw("targets", ""))).bungee()
                        );

                    var amount = args.getByClassOrDefault("amount", Double.class, 0D);
                    var point = pointManager.getPoints().get(key);
                    targets.forEach(target -> {
                        operation.execute(point, target.getUniqueId(), amount);

                        if (args.getByClassOrDefault("sender-message", Boolean.class, true))
                            I18n.global.accessor(sender, message).getPlaceholderComponent(target,
                                    Placeholder.replace("key", key),
                                    Placeholder.replace("amount", point.getVisualConfig().decimalFormat().format(amount)),
                                    Placeholder.replace("player-name", target.getName())
                            ).sendMessage(sender);

                        if (args.getByClassOrDefault("receiver-message", Boolean.class, true) && target.isOnline())
                            I18n.global.accessor(target, receiverMessage).getPlaceholderComponent(target,
                                    Placeholder.replace("key", key),
                                    Placeholder.replace("amount", point.getVisualConfig().decimalFormat().format(amount))
                            ).sendMessage(target.getPlayer());
                    });
                });
    }

    private CommandAPICommand exchangeCommand() {
         return new CommandAPICommand("exchange")
                 .withArguments(pointKeyArgument("key"))
                 .withArguments(CommandUtils.onlinePlayer("target"))
                 .withArguments(new DoubleArgument("amount", 1))
                 .withArguments(new GreedyStringArgument("execute"))
                 .executes((sender, args) -> {
                     var key = args.getOrDefaultRaw("key", "");
                     assertPointKeyExists(key, sender);
                     assertHasPermission(sender,  "points.exchange." + key);
                     var target = args.getByClass("target", Player.class);
                     var amount = args.getByClassOrDefault("amount", Double.class, 0D);
                     var point = pointManager.getPoints().get(key);
                     var command = (String) args.get("execute");
                     if (point.subtract(target, amount)) {
                         Bukkit.dispatchCommand(Bukkit.getConsoleSender(), I18n.global.accessor(target, command).getPlaceholderString(target).get());
                     }
                 });
    }

    @FunctionalInterface
    interface AddSetSub {
        void execute(Point point, UUID player, double amount);
    }

    private void assertHasPermission(CommandSender sender, String permission) throws WrapperCommandSyntaxException{
        if (sender.hasPermission(permission)) return;
        throw CommandAPIBukkit.failWithBaseComponents(
                I18n.global.accessor(sender, "error-permissions")
                        .getPlaceholderComponent(sender).bungee()
        );
    }

    private Argument<String> pointKeyArgument(String nodeName) {
        return new StringArgument(nodeName)
                .replaceSuggestions(ArgumentSuggestions.stringCollection(info -> pointManager.getPoints().keySet()));
    }

    private void assertPointKeyExists(String key, CommandSender sender) throws WrapperCommandSyntaxException {
        if (pointManager.getPoints().containsKey(key)) return;
        throw CommandAPIBukkit.failWithBaseComponents(
                I18n.global.accessor(sender, "error-point-not-found")
                        .getPlaceholderComponent(sender, Placeholder.replace("key", key)).bungee()
        );
    }

}
