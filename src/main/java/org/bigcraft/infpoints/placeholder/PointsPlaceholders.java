package org.bigcraft.infpoints.placeholder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.wyne.wutils.common.Args;
import me.wyne.wutils.common.placeholder.PAPIUtils;
import me.wyne.wutils.i18n.I18n;
import me.wyne.wutils.log.Log;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bigcraft.infpoints.InfPoints;
import org.bigcraft.infpoints.core.Point;
import org.bigcraft.infpoints.core.PointManager;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public class PointsPlaceholders extends PlaceholderExpansion {

    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();

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
            Log.global.error("Not enough arguments for points placeholder. Required: 2");
            return null;
        }

        if (!pointManager.getPoints().containsKey(pointKey)) {
            Log.global.error("Point '" + pointKey + "' doesn't exist (" + PAPIUtils.getPlaceholder(getIdentifier(), params) + ")");
            return null;
        }

        Point point = pointManager.getPoints().get(pointKey);

        switch (data) {
            case "name": return legacy.serialize(I18n.global.getComponent(I18n.toLocale(player), point.getVisualConfig().name()));
            case "name-plural": return legacy.serialize(I18n.global.getComponent(I18n.toLocale(player), point.getVisualConfig().pluralName()));
            case "symbol": return legacy.serialize(I18n.global.getComponent(I18n.toLocale(player), point.getVisualConfig().symbol()));
            case "color": return point.getVisualConfig().color();
            case "balance": return point.getVisualConfig().decimalFormat().format(point.get(player.getUniqueId()));
            case "balance-format": return formatNumber(point.getVisualConfig().decimalFormat().format(point.get(player.getUniqueId())));
            case "balance-int": return String.valueOf((int) point.get(player.getUniqueId()));
            case "balance-int-format": return formatNumber(String.valueOf((int) point.get(player.getUniqueId())));
        }

        Log.global.error("Placeholder '" + data + "' doesn't exist (" + PAPIUtils.getPlaceholder(getIdentifier(), params) + ")");
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
