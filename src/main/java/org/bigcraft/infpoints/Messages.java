package org.bigcraft.infpoints;

import me.wyne.wutils.i18n.I18n;
import me.wyne.wutils.i18n.language.replacement.Placeholder;
import me.wyne.wutils.i18n.language.replacement.TextReplacement;
import org.bigcraft.infpoints.api.Point;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class Messages {

    private Messages() {}

    public static void send(@NotNull CommandSender recipient, @NotNull String path, @NotNull TextReplacement... replacements) {
        I18n.global.accessor(recipient, path).getPlaceholderComponent(recipient, replacements).sendMessage(recipient);
    }

    // Messages about someone else resolve their placeholders with that player, so %player_name% and the
    // point placeholders describe the subject instead of whoever reads the message
    public static void sendAbout(@NotNull CommandSender recipient, @NotNull OfflinePlayer subject, @NotNull String path,
                                 @NotNull TextReplacement... replacements) {
        I18n.global.accessor(recipient, path).getPlaceholderComponent(subject, replacements).sendMessage(recipient);
    }

    public static @NotNull String format(@NotNull Point point, double amount) {
        return point.getVisualConfig().decimalFormat().format(amount);
    }

    public static @NotNull String name(@NotNull OfflinePlayer player) {
        String name = player.getName();
        return name != null ? name : player.getUniqueId().toString();
    }

    public static @NotNull TextReplacement key(@NotNull Point point) {
        return Placeholder.replace("key", point.getKey());
    }

    public static @NotNull TextReplacement amount(@NotNull Point point, double amount) {
        return Placeholder.replace("amount", format(point, amount));
    }

    public static @NotNull TextReplacement balance(@NotNull Point point, double balance) {
        return Placeholder.replace("balance", format(point, balance));
    }

    public static @NotNull TextReplacement playerName(@NotNull OfflinePlayer player) {
        return Placeholder.replace("player-name", name(player));
    }

}
