package org.bigcraft.infpoints.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import me.wyne.wutils.i18n.language.replacement.Placeholder;
import org.bigcraft.infpoints.Messages;
import org.bigcraft.infpoints.api.Point;
import org.bigcraft.infpoints.api.PointProvider;
import org.bigcraft.infpoints.api.transaction.Transaction;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

public final class HistoryCommand extends SubCommand {

    private static final int PAGE_SIZE = 10;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final PointProvider points;

    public HistoryCommand(@NotNull PointProvider points) {
        super("history");
        this.points = points;
    }

    @Override
    protected @NotNull CommandAPICommand build(@NotNull CommandAPICommand command) {
        return command
                .withArguments(CustomArguments.pointArgument("key", points))
                .withOptionalArguments(CustomArguments.targetsArgument("target"), new IntegerArgument("page", 1))
                .executes(this::execute);
    }

    private void execute(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
        Point point = CommandSupport.point(args);
        Player self = sender instanceof Player player ? player : null;
        OfflinePlayer target;
        if (args.getRawOptional("target").isEmpty()) {
            if (self == null)
                throw CommandSupport.fail(sender, "error-player-only");
            target = self;
        } else {
            target = CommandSupport.resolveTarget(sender, args, "target");
        }
        boolean own = self != null && target.getUniqueId().equals(self.getUniqueId());
        CommandSupport.assertPermission(sender, (own ? "points.history." : "points.history-other.") + point.getKey());
        if (!point.supportsHistory())
            throw CommandSupport.fail(sender, "error-history-unsupported", Messages.key(point));
        if (!point.isAvailable())
            throw CommandSupport.fail(sender, "error-point-unavailable", Messages.key(point));
        int page = args.getByClassOrDefault("page", Integer.class, 1);
        CommandSupport.onMainThread(point.async().history(target.getUniqueId(), (page - 1) * PAGE_SIZE, PAGE_SIZE), transactions -> {
            Messages.sendAbout(sender, target, "info-history-header", Messages.key(point), Messages.playerName(target), Placeholder.replace("page", page));
            if (transactions.isEmpty()) {
                Messages.send(sender, "info-history-empty", Messages.key(point));
                return;
            }
            for (Transaction transaction : transactions) {
                Messages.send(sender, "info-history-entry", Messages.key(point),
                        Placeholder.replace("id", transaction.id()),
                        Placeholder.replace("date", DATE_FORMAT.format(transaction.createdAt())),
                        Placeholder.replace("type", transaction.type().name().toLowerCase(Locale.ROOT)),
                        Placeholder.replace("amount", (transaction.amount() > 0 ? "+" : "") + Messages.format(point, transaction.amount())),
                        Messages.balance(point, transaction.balance()),
                        Placeholder.replace("source", Objects.requireNonNullElse(transaction.source(), "-")),
                        Placeholder.replace("reason", Objects.requireNonNullElse(transaction.reason(), "-")));
            }
        });
    }

}
