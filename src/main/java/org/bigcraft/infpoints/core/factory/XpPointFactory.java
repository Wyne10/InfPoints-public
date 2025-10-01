package org.bigcraft.infpoints.core.factory;

import com.google.inject.Singleton;
import org.bigcraft.infpoints.InfPoints;
import org.bigcraft.infpoints.api.event.PointAddEvent;
import org.bigcraft.infpoints.api.event.PointEvent;
import org.bigcraft.infpoints.api.event.PointEventType;
import org.bigcraft.infpoints.core.Point;
import org.bigcraft.infpoints.core.XpPoint;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLevelChangeEvent;

@Singleton
public class XpPointFactory implements PointFactory, Listener {

    private XpPoint xpPoint;

    public XpPointFactory() {
        Bukkit.getPluginManager().registerEvents(this, InfPoints.getInstance());
    }

    @Override
    public Point create(ConfigurationSection config) {
        if (xpPoint != null)
            return xpPoint;
        this.xpPoint = new XpPoint(config);
        return xpPoint;
    }

    @EventHandler(ignoreCancelled = true)
    private void onLevelChange(PlayerLevelChangeEvent e) {
        if (xpPoint == null) return;
        PointEvent event;
        int newLevel = e.getNewLevel();
        int amount;
        if (newLevel > e.getOldLevel()) {
            amount = newLevel - e.getOldLevel();
            event = PointEventType.ADD.call(xpPoint, xpPoint, e.getPlayer().getUniqueId(), amount);
        } else if (newLevel < e.getOldLevel()) {
            amount = e.getOldLevel() - newLevel;
            event = PointEventType.SUBTRACT.call(xpPoint, xpPoint, e.getPlayer().getUniqueId(), amount);
        } else return;

        if (!event.callEvent())
            newLevel = e.getOldLevel();
        else {
            if (((int) event.getAmount()) == amount) return;
            if (newLevel > e.getOldLevel())
                newLevel = e.getOldLevel() + (int) event.getAmount();
            else if (newLevel < e.getOldLevel())
                newLevel = e.getOldLevel() - (int) event.getAmount();
        }
        e.getPlayer().setLevel(newLevel);
    }

}
