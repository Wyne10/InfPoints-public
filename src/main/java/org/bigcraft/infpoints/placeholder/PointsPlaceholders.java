package org.bigcraft.infpoints.placeholder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.wyne.wutils.common.Args;
import me.wyne.wutils.common.placeholder.PAPIUtils;
import me.wyne.wutils.i18n.I18n;
import me.wyne.wutils.i18n.language.component.PlaceholderLocalizedComponent;
import me.wyne.wutils.i18n.language.interpretation.ComponentInterpreters;
import me.wyne.wutils.i18n.language.interpretation.LegacyInterpreter;
import me.wyne.wutils.i18n.language.validation.EmptyValidator;
import org.bigcraft.infpoints.InfPoints;
import org.bigcraft.infpoints.core.Point;
import org.bigcraft.infpoints.core.PointManager;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public class PointsPlaceholders extends PlaceholderExpansion {

    private final InfPoints plugin;
    private final PointManager pointManager;

    @Inject
    public PointsPlaceholders(InfPoints plugin, PointManager pointManager) {
        this.plugin = plugin;
        this.pointManager = pointManager;
        register();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "points";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Wyne";
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
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        Args args = new Args(params, "_");
        String pointKey = args.get(0);
        String data = args.get(1);

        if (args.size() < 2) {
            InfPoints.getInstance().getLog().error("Not enough arguments for points placeholder. Required: 2");
            return null;
        }

        if (!pointManager.getPoints().containsKey(pointKey)) {
            InfPoints.getInstance().getLog().error("Point '{}' doesn't exist ({})", pointKey, PAPIUtils.getPlaceholder(getIdentifier(), params));
            return null;
        }

        Point point = pointManager.getPoints().get(pointKey);

        if (data.startsWith("name-plural"))
            return I18n.global.accessor(player, point.getVisualConfig().pluralName())
                    .getPlaceholderComponent(player).style("name-plural", data);
        else if (data.startsWith("name"))
            return I18n.global.accessor(player, point.getVisualConfig().name())
                    .getPlaceholderComponent(player).style("name", data);
        else if (data.startsWith("symbol"))
            return I18n.global.accessor(player, point.getVisualConfig().symbol())
                    .getPlaceholderComponent(player).style("symbol", data);
        else if (data.equals("color"))
            return point.getVisualConfig().color();
        else if (data.startsWith("color"))
            return new PlaceholderLocalizedComponent(
                    ComponentInterpreters.LEGACY.get(new EmptyValidator()),
                    I18n.global.getLanguage(I18n.toLocale(player)),
                    point.getVisualConfig().color(),
                    LegacyInterpreter.SERIALIZER.deserialize(point.getVisualConfig().color()),
                    I18n.global.getAudiences(),
                    player
            ).style("color", data);

        switch (data) {
            case "balance": return point.getVisualConfig().decimalFormat().format(point.get(player.getUniqueId()));
            case "balance-format": return formatNumber(point.getVisualConfig().decimalFormat().format(point.get(player.getUniqueId())));
            case "balance-int": return String.valueOf((int) point.get(player.getUniqueId()));
            case "balance-int-format": return formatNumber(String.valueOf((int) point.get(player.getUniqueId())));
        }

        InfPoints.getInstance().getLog().error("Placeholder '{}' doesn't exist ({})", data, PAPIUtils.getPlaceholder(getIdentifier(), params));
        return null;
    }

    private String formatNumber(String number) {
        boolean containsDecimal = number.contains(".");
        StringBuilder sb = new StringBuilder(number.substring(0, containsDecimal ? number.lastIndexOf('.') : number.length()));
        int length = sb.length();

        for (int i = length - 3; i > 0; i -= 3) {
            sb.insert(i, ",");
        }

        return sb + (containsDecimal ? number.substring(number.lastIndexOf('.')) : "");
    }

}
