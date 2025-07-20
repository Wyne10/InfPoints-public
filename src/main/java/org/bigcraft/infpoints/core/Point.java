package org.bigcraft.infpoints.core;

import lombok.Getter;
import me.wyne.wutils.i18n.language.interpretation.LegacyInterpreter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bigcraft.infpoints.api.config.CommandConfig;
import org.bigcraft.infpoints.api.config.VisualConfig;
import org.bukkit.configuration.ConfigurationSection;

import java.util.UUID;

@Getter
public abstract class Point implements org.bigcraft.infpoints.api.Point {

    private final org.bigcraft.infpoints.api.config.PointConfig config;
    private final VisualConfig visualConfig;
    private final String colorMiniMessage;
    private final CommandConfig commandConfig;

    public Point(ConfigurationSection config) {
        this.config = org.bigcraft.infpoints.api.config.PointConfig.fromConfig(config);
        this.visualConfig = VisualConfig.fromConfig(config);
        this.colorMiniMessage = MiniMessage.miniMessage().serialize(LegacyInterpreter.SERIALIZER.deserialize(visualConfig.color()));
        this.commandConfig = CommandConfig.fromConfig(config);
    }

    protected Point(Point point) {
        this.config = point.config;
        this.visualConfig = point.visualConfig;
        this.colorMiniMessage = point.colorMiniMessage;
        this.commandConfig = point.commandConfig;
    }

    @Override
    public String getFormat(UUID player) {
        return getVisualConfig().decimalFormat().format(get(player));
    }

    @Override
    public void add(UUID player, double amount) {
        set(player, get(player) + amount);
    }

    @Override
    public boolean subtract(UUID player, double amount) {
        if (get(player) < amount)
            return false;
        set(player, get(player) - amount);
        return true;
    }

    @Override
    public boolean transfer(UUID sender, UUID receiver, double amount) {
        if (!subtract(sender, amount))
            return false;
        add(receiver, amount);
        return true;
    }

}
