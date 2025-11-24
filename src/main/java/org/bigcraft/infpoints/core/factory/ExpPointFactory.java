package org.bigcraft.infpoints.core.factory;

import com.google.inject.Singleton;
import org.bigcraft.infpoints.InfPoints;
import org.bigcraft.infpoints.api.PointTypes;
import org.bigcraft.infpoints.api.event.PointEventType;
import org.bigcraft.infpoints.api.event.PointSubtractEvent;
import org.bigcraft.infpoints.core.ExpPoint;
import org.bigcraft.infpoints.core.Point;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;

@Singleton
public class ExpPointFactory implements PointFactory, Listener {

    private ExpPoint expPoint;

    public ExpPointFactory() {
        Bukkit.getPluginManager().registerEvents(this, InfPoints.getInstance());
    }

    @Override
    public Point create(ConfigurationSection config) {
        if (expPoint != null)
            return expPoint;
        this.expPoint = new ExpPoint(config);
        return expPoint;
    }

    @EventHandler(ignoreCancelled = true)
    private void onLevelSubtract(PointSubtractEvent e) {
        if (expPoint == null) return;
        if (e.getPointConfig().getConfig().pointType() != PointTypes.LEVEL) return;
        var amount = ExpPoint.levelToExp((int) e.getBalance()) - ExpPoint.levelToExp((int) e.getNewBalance());
        var event = PointEventType.SUBTRACT.create(expPoint, e.getPlayer(), amount);
        if (event.callEvent()) {
            if (((int) event.getAmount()) == amount)
                return;
            e.setAmount(ExpPoint.expToLevel((int) event.getAmount()));
        } else
            e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onExpChange(PlayerExpChangeEvent e) {
        if (expPoint == null) return;
        var event = PointEventType.ADD.create(expPoint, e.getPlayer().getUniqueId(), e.getAmount());
        if (event.callEvent()) {
            e.setAmount((int) event.getAmount());
        } else
            e.setAmount(0);
    }

}
