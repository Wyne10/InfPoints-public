package org.bigcraft.infpoints.point;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bigcraft.infpoints.InfPoints;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@Singleton
public final class CacheListener implements Listener {

    private final PointManager points;
    private final BalanceCache cache;

    @Inject
    public CacheListener(InfPoints plugin, PointManager points, BalanceCache cache) {
        this.points = points;
        this.cache = cache;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // Login is already asynchronous, so balances are loaded here and placeholders have them right after joining
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED)
            return;
        points.getHandles().forEach(handle -> handle.preload(event.getUniqueId()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (cache.isEvictOnQuit())
            cache.evict(event.getPlayer().getUniqueId());
    }

}
