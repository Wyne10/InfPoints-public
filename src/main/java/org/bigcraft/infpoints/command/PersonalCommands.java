package org.bigcraft.infpoints.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.jorel.commandapi.CommandAPIBukkit;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.DoubleArgument;
import me.wyne.wutils.common.loadable.Loadable;
import me.wyne.wutils.common.loadable.LoadableMeta;
import me.wyne.wutils.common.loadable.Loader;
import org.bigcraft.infpoints.InfPoints;
import org.bigcraft.infpoints.api.config.CommandConfig;
import org.bigcraft.infpoints.point.PointHandle;
import org.bigcraft.infpoints.point.PointManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

// Per-point commands like /pay and /balance, registered from each point's configuration
@Singleton
@LoadableMeta(priority = 1)
public final class PersonalCommands implements Loadable {

    private final InfPoints plugin;
    private final PointManager points;
    private final List<String> registered = new ArrayList<>();

    @Inject
    public PersonalCommands(InfPoints plugin, PointManager points) {
        this.plugin = plugin;
        this.points = points;
        Loader.global.registerLoadable(this);
    }

    @Override
    public void load(@NotNull ConfigurationSection config) {
        registered.forEach(name -> CommandAPIBukkit.unregister(name, true, true));
        registered.clear();
        for (PointHandle point : points.getHandles()) {
            CommandConfig commands = point.getCommandConfig();
            if (commands.usePayCommand()) {
                new CommandAPICommand(commands.payCommand())
                        .withAliases(commands.payAliases().toArray(String[]::new))
                        .withArguments(CustomArguments.targetsArgument("target"), new DoubleArgument("amount"))
                        .executesPlayer((player, args) -> {
                            PayCommand.execute(player, point, args);
                        })
                        .register(plugin);
                registered.add(commands.payCommand());
                registered.addAll(commands.payAliases());
            }
            if (commands.useBalanceCommand()) {
                new CommandAPICommand(commands.balanceCommand())
                        .withAliases(commands.balanceAliases().toArray(String[]::new))
                        .withOptionalArguments(CustomArguments.targetsArgument("target"))
                        .executes((sender, args) -> {
                            BalanceCommand.execute(sender, point, args);
                        })
                        .register(plugin);
                registered.add(commands.balanceCommand());
                registered.addAll(commands.balanceAliases());
            }
        }
        Bukkit.getOnlinePlayers().forEach(Player::updateCommands);
    }

}
