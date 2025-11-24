package org.bigcraft.infpoints.core.factory;

import com.google.inject.Singleton;
import org.bigcraft.infpoints.InfPoints;
import org.bigcraft.infpoints.api.event.PointEvent;
import org.bigcraft.infpoints.api.event.PointEventType;
import org.bigcraft.infpoints.core.Point;
import org.bigcraft.infpoints.core.LevelPoint;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLevelChangeEvent;

@Singleton
public class LevelPointFactory implements PointFactory, Listener {

    private LevelPoint levelPoint;

    public LevelPointFactory() {
        Bukkit.getPluginManager().registerEvents(this, InfPoints.getInstance());
    }

    @Override
    public Point create(ConfigurationSection config) {
        if (levelPoint != null)
            return levelPoint;
        this.levelPoint = new LevelPoint(config);
        return levelPoint;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onLevelChange(PlayerLevelChangeEvent e) {
        if (levelPoint == null) return;
        e.getPlayer().setLevel(e.getOldLevel());
        PointEvent event;
        int newLevel = e.getNewLevel();
        int amount;
        if (newLevel > e.getOldLevel()) {
            amount = newLevel - e.getOldLevel();
            event = PointEventType.ADD.create(levelPoint, e.getPlayer().getUniqueId(), amount);
        } else if (newLevel < e.getOldLevel()) {
            amount = e.getOldLevel() - newLevel;
            event = PointEventType.SUBTRACT.create(levelPoint, e.getPlayer().getUniqueId(), amount);
        } else return;

        if (event.callEvent()) {
            if (((int) event.getAmount()) == amount) {
                e.getPlayer().setLevel(newLevel);
                return;
            }

            if (newLevel > e.getOldLevel())
                newLevel = e.getOldLevel() + (int) event.getAmount();
            else if (newLevel < e.getOldLevel())
                newLevel = e.getOldLevel() - (int) event.getAmount();
            e.getPlayer().setLevel(newLevel);
        }
    }

}
