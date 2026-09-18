package me.wyne.infpoints.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import me.wyne.wutils.i18n.I18n;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class HelpCommand extends SubCommand {

    public HelpCommand() {
        super("help");
    }

    @Override
    protected @NotNull CommandAPICommand build(@NotNull CommandAPICommand command) {
        return command.executes(HelpCommand::sendHelp);
    }

    public static void sendHelp(@NotNull CommandSender sender, @NotNull CommandArguments args) {
        I18n.global.accessor(sender, "info-help").getPlaceholderComponentList(sender).forEach(line -> line.sendMessage(sender));
    }

}
