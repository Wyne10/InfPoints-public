package org.bigcraft.infpoints.placeholder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.wyne.wutils.config.Config;
import me.wyne.wutils.config.ConfigEntry;
import me.wyne.wutils.i18n.I18n;
import me.wyne.wutils.i18n.language.component.PlaceholderLocalizedComponent;
import me.wyne.wutils.i18n.language.interpretation.ComponentInterpreters;
import me.wyne.wutils.i18n.language.interpretation.LegacyInterpreter;
import me.wyne.wutils.i18n.language.validation.EmptyValidator;
import org.bigcraft.infpoints.InfPoints;
import org.bigcraft.infpoints.Messages;
import org.bigcraft.infpoints.api.config.VisualConfig;
import org.bigcraft.infpoints.point.PointHandle;
import org.bigcraft.infpoints.point.PointManager;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalDouble;

@SuppressWarnings("FieldMayBeFinal")
@Singleton
public final class PointsPlaceholders extends PlaceholderExpansion {

    @ConfigEntry(section = "Placeholders", comment = "Text balance placeholders show until the balance is loaded")
    private String loading = "...";

    private final InfPoints plugin;
    private final PointManager points;

    @Inject
    public PointsPlaceholders(InfPoints plugin, PointManager points) {
        this.plugin = plugin;
        this.points = points;
        Config.global.registerConfigObject(this);
        register();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "points";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer player, @NotNull String params) {
        PointHandle point = findPoint(params);
        if (point == null)
            return null;
        String data = params.substring(point.getKey().length() + 1);
        VisualConfig visual = point.getVisualConfig();

        if (data.startsWith("name-plural"))
            return I18n.global.accessor(player, visual.pluralName()).getPlaceholderComponent(player).style("name-plural", data);
        if (data.startsWith("name"))
            return I18n.global.accessor(player, visual.name()).getPlaceholderComponent(player).style("name", data);
        if (data.startsWith("symbol"))
            return I18n.global.accessor(player, visual.symbol()).getPlaceholderComponent(player).style("symbol", data);
        if (data.equals("color"))
            return visual.color();
        if (data.startsWith("color"))
            return new PlaceholderLocalizedComponent(
                    ComponentInterpreters.LEGACY.get(new EmptyValidator()),
                    I18n.global.getLanguage(I18n.toLocale(player)),
                    visual.color(),
                    LegacyInterpreter.SERIALIZER.deserialize(visual.color()),
                    I18n.global.getAudiences(),
                    player
            ).style("color", data);

        if (player == null)
            return null;
        OptionalDouble cached = point.getCached(player.getUniqueId());
        if (cached.isEmpty())
            return loading;
        double balance = cached.getAsDouble();
        return switch (data) {
            case "balance" -> Messages.format(point, balance);
            case "balance-format" -> groupThousands(Messages.format(point, balance));
            case "balance-int" -> String.valueOf((long) balance);
            case "balance-int-format" -> groupThousands(String.valueOf((long) balance));
            default -> {
                InfPoints.logger().debug("Unknown placeholder %points_{}%", params);
                yield null;
            }
        };
    }

    // The longest matching key wins, so keys containing '_' work
    private @Nullable PointHandle findPoint(String params) {
        String match = null;
        for (String key : points.getKeys()) {
            if (params.startsWith(key + "_") && (match == null || key.length() > match.length()))
                match = key;
        }
        return match == null ? null : points.getHandle(match);
    }

    private static String groupThousands(String number) {
        boolean containsDecimal = number.contains(".");
        StringBuilder builder = new StringBuilder(number.substring(0, containsDecimal ? number.lastIndexOf('.') : number.length()));
        int start = builder.length() > 0 && builder.charAt(0) == '-' ? 1 : 0;
        for (int i = builder.length() - 3; i > start; i -= 3)
            builder.insert(i, ",");
        return builder + (containsDecimal ? number.substring(number.lastIndexOf('.')) : "");
    }

}
