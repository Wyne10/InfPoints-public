package org.bigcraft.infpoints.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.jorel.commandapi.CommandAPIBukkit;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import me.wyne.wutils.i18n.I18n;
import me.wyne.wutils.i18n.language.replacement.Placeholder;
import org.bigcraft.infpoints.core.Point;
import org.bigcraft.infpoints.core.PointManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Singleton
public class InfPointsCommand {

    private final PointManager pointManager;

    @Inject
    public InfPointsCommand(PointManager pointManager) {
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
                                    Point point = getPoint(sender, args);
                                    if (point.getConfig().canCheckBalance())
                                        sender.sendMessage(I18n.global.getPlaceholderComponent(
                                                sender.locale(),
                                                sender,
                                            "info-point-balance",
                                                Placeholder.replace("key", args.getRaw("point"))
                                        ));
                                })
                                .then(new EntitySelectorArgument.OnePlayer("target")
                                        .executes((sender, args) -> {
                                            Player player = args.getByClass("target", Player.class);
                                            Point point = getPoint(sender, args);
                                            if (point.getConfig().canCheckBalance())
                                                sender.sendMessage(I18n.global.getPlaceholderComponent(
                                                        I18n.toLocale(sender),
                                                        player,
                                                        "info-point-balance-other",
                                                        Placeholder.replace("key", args.getRaw("point"))
                                                ));
                                }))))
                .register();
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
