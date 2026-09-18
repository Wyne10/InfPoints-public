package me.wyne.infpoints.command;

import dev.jorel.commandapi.CommandAPIBukkit;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import me.wyne.wutils.common.scheduler.Schedulers;
import me.wyne.wutils.i18n.I18n;
import me.wyne.wutils.i18n.language.replacement.Placeholder;
import me.wyne.wutils.i18n.language.replacement.TextReplacement;
import me.wyne.infpoints.InfPoints;
import me.wyne.infpoints.Messages;
import me.wyne.infpoints.api.Point;
import me.wyne.infpoints.api.transaction.TransactionResult;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class CommandSupport {

    private CommandSupport() {}

    public static @NotNull WrapperCommandSyntaxException fail(@NotNull CommandSender sender, @NotNull String path, @NotNull TextReplacement... replacements) {
        return CommandAPIBukkit.failWithBaseComponents(I18n.global.accessor(sender, path).getPlaceholderComponent(sender, replacements).bungee());
    }

    public static void assertPermission(@NotNull CommandSender sender, @NotNull String permission) throws WrapperCommandSyntaxException {
        if (!sender.hasPermission(permission))
            throw fail(sender, "error-permissions");
    }

    public static @NotNull Point point(@NotNull CommandArguments args) {
        return (Point) Objects.requireNonNull(args.get("key"));
    }

    public static @Nullable UUID actor(@NotNull CommandSender sender) {
        return sender instanceof Player player ? player.getUniqueId() : null;
    }

    // A selector resolves to the online players it selects; anything else is a UUID or the name of a player who has joined before
    public static @NotNull List<OfflinePlayer> resolveTargets(@NotNull CommandSender sender, @NotNull CommandArguments args, @NotNull String node) throws WrapperCommandSyntaxException {
        String input = Objects.requireNonNullElse(args.getRaw(node), "");
        if (input.startsWith("@")) {
            List<OfflinePlayer> players = new ArrayList<>();
            if (args.get(node) instanceof Collection<?> selected) {
                for (Object entity : selected) {
                    if (entity instanceof Player player)
                        players.add(player);
                }
            }
            if (players.isEmpty())
                throw fail(sender, "error-player-not-found", Placeholder.replace("name", input));
            return players;
        }
        OfflinePlayer player = resolvePlayer(input);
        if (player == null)
            throw fail(sender, "error-player-not-found", Placeholder.replace("name", input));
        return List.of(player);
    }

    public static @NotNull OfflinePlayer resolveTarget(@NotNull CommandSender sender, @NotNull CommandArguments args, @NotNull String node) throws WrapperCommandSyntaxException {
        List<OfflinePlayer> targets = resolveTargets(sender, args, node);
        if (targets.size() != 1)
            throw fail(sender, "error-single-target");
        return targets.get(0);
    }

    public static void reportFailure(@NotNull CommandSender sender, @NotNull Point point, @NotNull TransactionResult result, @NotNull OfflinePlayer target) {
        String path = switch (result.status()) {
            case INSUFFICIENT_FUNDS -> "error-insufficient-funds-other";
            case INVALID_AMOUNT -> "error-invalid-amount";
            case PLAYER_OFFLINE -> "error-player-offline";
            case CANCELLED -> "error-transaction-cancelled";
            case UNAVAILABLE -> "error-point-unavailable";
            default -> "error-transaction-failed";
        };
        Messages.sendAbout(sender, target, path, Messages.key(point), Messages.playerName(target), Messages.amount(point, result.amount()));
    }

    // Replies are skipped once the plugin is disabling, Bukkit refuses to schedule them then
    public static <T> void onMainThread(@NotNull CompletableFuture<T> future, @NotNull Consumer<T> callback) {
        future.thenAcceptAsync(callback, CommandSupport::runOnMainThread).exceptionally(throwable -> {
            InfPoints.logger().error("InfPoints command callback failed", throwable);
            return null;
        });
    }

    private static void runOnMainThread(Runnable runnable) {
        if (InfPoints.getInstance().isEnabled())
            Schedulers.sync().execute(runnable);
    }

    private static @Nullable OfflinePlayer resolvePlayer(String input) {
        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(input));
        } catch (IllegalArgumentException ignored) {
        }
        Player online = Bukkit.getPlayerExact(input);
        if (online != null)
            return online;
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(input);
        return cached != null && cached.hasPlayedBefore() ? cached : null;
    }

}
