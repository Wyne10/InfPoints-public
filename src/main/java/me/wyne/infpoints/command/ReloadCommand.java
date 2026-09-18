package me.wyne.infpoints.command;

import dev.jorel.commandapi.CommandAPICommand;
import me.wyne.infpoints.InfPoints;
import me.wyne.infpoints.Messages;
import org.jetbrains.annotations.NotNull;

public final class ReloadCommand extends SubCommand {

    private final InfPoints plugin;

    public ReloadCommand(@NotNull InfPoints plugin) {
        super("reload");
        this.plugin = plugin;
    }

    @Override
    protected @NotNull CommandAPICommand build(@NotNull CommandAPICommand command) {
        return command
                .withPermission("points.admin.reload")
                .executes((sender, args) -> {
                    plugin.reload();
                    Messages.send(sender, "success-plugin-reload");
                    InfPoints.logger().info("Plugin reloaded by {}", sender.getName());
                });
    }

}
