package me.wyne.infpoints.command;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import me.wyne.wutils.i18n.I18n;
import me.wyne.wutils.i18n.language.replacement.Placeholder;
import me.wyne.infpoints.api.Point;
import me.wyne.infpoints.api.PointProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class CustomArguments {

    private CustomArguments() {}

    public static @NotNull Argument<Point> pointArgument(@NotNull String nodeName, @NotNull PointProvider points) {
        return new CustomArgument<>(new StringArgument(nodeName), info -> {
            Point point = points.getPoint(info.input());
            if (point == null)
                throw CustomArgument.CustomArgumentException.fromBaseComponents(I18n.global.accessor(info.sender(), "error-point-not-found")
                        .getPlaceholderComponent(info.sender(), Placeholder.replace("key", info.input())).bungee());
            return point;
        }).replaceSuggestions(ArgumentSuggestions.stringCollection(info -> points.getKeys()));
    }

    // An entity selector because the players-only one rejects UUIDs; it parses selectors like @a as well as names and UUIDs,
    // which select nothing while the player is offline, see CommandSupport#resolveTargets
    public static @NotNull Argument<?> targetsArgument(@NotNull String nodeName) {
        return new EntitySelectorArgument.ManyEntities(nodeName)
                .replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
                    List<String> suggestions = new ArrayList<>(List.of("@a", "@p", "@r", "@s"));
                    Bukkit.getOnlinePlayers().stream().map(Player::getName).forEach(suggestions::add);
                    return suggestions;
                }));
    }

}
