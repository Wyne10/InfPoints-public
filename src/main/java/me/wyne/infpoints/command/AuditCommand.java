package me.wyne.infpoints.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import me.wyne.wutils.i18n.language.replacement.Placeholder;
import me.wyne.infpoints.InfPoints;
import me.wyne.infpoints.Messages;
import me.wyne.infpoints.api.Point;
import me.wyne.infpoints.point.Amounts;
import me.wyne.infpoints.point.PointHandle;
import me.wyne.infpoints.point.PointManager;
import me.wyne.infpoints.storage.AuditReport;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

// Adds up each account's transaction history and reports stored balances that don't match it
public final class AuditCommand extends SubCommand {

    private static final int SHOWN_MISMATCHES = 10;

    private final PointManager points;

    public AuditCommand(@NotNull PointManager points) {
        super("audit");
        this.points = points;
    }

    @Override
    protected @NotNull CommandAPICommand build(@NotNull CommandAPICommand command) {
        return command
                .withPermission("points.admin.audit")
                .withArguments(CustomArguments.pointArgument("key", points))
                .withOptionalArguments(CustomArguments.targetsArgument("target"))
                .executes(this::execute);
    }

    private void execute(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
        Point point = CommandSupport.point(args);
        PointHandle handle = points.getHandle(point.getKey());
        if (handle == null || !point.supportsHistory())
            throw CommandSupport.fail(sender, "error-history-unsupported", Messages.key(point));
        UUID player = args.getRawOptional("target").isPresent() ? CommandSupport.resolveTarget(sender, args, "target").getUniqueId() : null;
        int decimals = point.getConfig().decimals();
        CommandSupport.onMainThread(handle.audit(player), report -> {
            if (report.isEmpty()) {
                Messages.send(sender, "error-audit-failed", Messages.key(point));
                return;
            }
            AuditReport audit = report.get();
            Messages.send(sender, "info-audit-result", Messages.key(point), Placeholder.replace("checked", audit.checked()),
                    Placeholder.replace("mismatches", audit.mismatches().size()));
            audit.mismatches().stream().limit(SHOWN_MISMATCHES).forEach(mismatch -> Messages.send(sender, "info-audit-mismatch", Messages.key(point),
                    Messages.playerName(Bukkit.getOfflinePlayer(mismatch.player())),
                    Messages.balance(point, Amounts.toAmount(mismatch.balance(), decimals)),
                    Placeholder.replace("ledger", Messages.format(point, Amounts.toAmount(mismatch.ledger(), decimals)))));
            if (!audit.mismatches().isEmpty())
                InfPoints.logger().warn("Audit of point '{}' found {} balances that don't match their history: {}", point.getKey(),
                        audit.mismatches().size(), audit.mismatches());
        });
    }

}
