package me.wyne.infpoints.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import me.wyne.infpoints.Messages;
import me.wyne.infpoints.api.Point;
import me.wyne.infpoints.api.PointProvider;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class BalanceCommand extends SubCommand {

    private final PointProvider points;

    public BalanceCommand(@NotNull PointProvider points) {
        super("balance");
        this.points = points;
    }

    @Override
    protected @NotNull CommandAPICommand build(@NotNull CommandAPICommand command) {
        return command
                .withArguments(CustomArguments.pointArgument("key", points))
                .withOptionalArguments(CustomArguments.targetsArgument("target"))
                .executes((sender, args) -> {
                    execute(sender, CommandSupport.point(args), args);
                });
    }

    static void execute(@NotNull CommandSender sender, @NotNull Point point, @NotNull CommandArguments args) throws WrapperCommandSyntaxException {
        Player self = sender instanceof Player player ? player : null;
        List<OfflinePlayer> targets;
        if (args.getRawOptional("target").isEmpty()) {
            if (self == null)
                throw CommandSupport.fail(sender, "error-player-only");
            targets = List.of(self);
        } else {
            targets = CommandSupport.resolveTargets(sender, args, "target");
        }
        boolean own = self != null && targets.size() == 1 && targets.get(0).getUniqueId().equals(self.getUniqueId());
        CommandSupport.assertPermission(sender, (own ? "points.balance." : "points.balance-other.") + point.getKey());
        if (!point.isAvailable())
            throw CommandSupport.fail(sender, "error-point-unavailable", Messages.key(point));
        for (OfflinePlayer target : targets) {
            if (!point.supportsOfflinePlayers() && !target.isOnline()) {
                Messages.sendAbout(sender, target, "error-player-offline", Messages.key(point), Messages.playerName(target));
                continue;
            }
            CommandSupport.onMainThread(point.async().get(target.getUniqueId()), balance -> {
                if (own)
                    Messages.send(sender, "info-point-balance", Messages.key(point), Messages.balance(point, balance));
                else
                    Messages.sendAbout(sender, target, "info-point-balance-other", Messages.key(point), Messages.playerName(target), Messages.balance(point, balance));
            });
        }
    }

}
