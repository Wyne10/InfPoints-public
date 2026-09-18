package me.wyne.infpoints.storage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.wyne.wutils.common.player.PlayerUtils;
import me.wyne.infpoints.InfPoints;
import me.wyne.infpoints.api.PointStorageType;
import me.wyne.infpoints.api.event.PointAddEvent;
import me.wyne.infpoints.api.event.PointEvent;
import me.wyne.infpoints.api.event.PointSubtractEvent;
import me.wyne.infpoints.api.transaction.TransactionRequest;
import me.wyne.infpoints.point.PointHandle;
import me.wyne.infpoints.point.PointManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;

// Routes vanilla experience changes through point events, so listeners can cancel or change them like any other balance change
@Singleton
public final class VanillaExperienceBridge implements Listener {

    private static final String SOURCE = "minecraft";

    private final PointManager points;
    private final ExperienceChanges changes;

    @Inject
    public VanillaExperienceBridge(InfPoints plugin, PointManager points, ExperienceChanges changes) {
        this.points = points;
        this.changes = changes;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLevelChange(PlayerLevelChangeEvent event) {
        Player player = event.getPlayer();
        if (changes.consume(player.getUniqueId(), event.getNewLevel()))
            return;
        PointHandle levelPoint = points.firstOfType(PointStorageType.LEVEL);
        int oldLevel = event.getOldLevel();
        int newLevel = event.getNewLevel();
        if (levelPoint == null || !levelPoint.isAvailable() || oldLevel == newLevel)
            return;
        boolean increase = newLevel > oldLevel;
        int amount = Math.abs(newLevel - oldLevel);
        TransactionRequest request = increase
                ? TransactionRequest.add(player.getUniqueId(), amount).withSource(SOURCE)
                : TransactionRequest.subtract(player.getUniqueId(), amount).withSource(SOURCE);
        player.setLevel(oldLevel);
        PointEvent pointEvent = increase ? new PointAddEvent(levelPoint, request) : new PointSubtractEvent(levelPoint, request);
        if (!pointEvent.callEvent())
            return;
        int applied = (int) Math.round(pointEvent.getAmount());
        player.setLevel(increase ? oldLevel + applied : Math.max(0, oldLevel - applied));
    }

    @EventHandler(ignoreCancelled = true)
    public void onLevelSubtract(PointSubtractEvent event) {
        if (event.getPoint().getConfig().type() != PointStorageType.LEVEL)
            return;
        PointHandle expPoint = points.firstOfType(PointStorageType.EXP);
        Player player = Bukkit.getPlayer(event.getPlayer());
        if (expPoint == null || !expPoint.isAvailable() || player == null)
            return;
        int level = player.getLevel();
        int levels = (int) Math.round(event.getAmount());
        long exp = PlayerUtils.levelToExp(level) - PlayerUtils.levelToExp(Math.max(0, level - levels));
        PointSubtractEvent expEvent = new PointSubtractEvent(expPoint, TransactionRequest.subtract(player.getUniqueId(), exp).withSource(SOURCE));
        if (!expEvent.callEvent()) {
            event.setCancelled(true);
            return;
        }
        long changed = Math.round(expEvent.getAmount());
        if (changed == exp)
            return;
        // The changed amount is experience to remove, so the levels lost are the ones below what remains
        long remainingLevel = PlayerUtils.expToLevel(Math.max(0, PlayerUtils.levelToExp(level) - changed));
        long changedLevels = level - remainingLevel;
        if (changedLevels <= 0)
            event.setCancelled(true);
        else
            event.setAmount(changedLevels);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onExpChange(PlayerExpChangeEvent event) {
        PointHandle expPoint = points.firstOfType(PointStorageType.EXP);
        if (expPoint == null || !expPoint.isAvailable())
            return;
        PointAddEvent pointEvent = new PointAddEvent(expPoint, TransactionRequest.add(event.getPlayer().getUniqueId(), event.getAmount()).withSource(SOURCE));
        event.setAmount(pointEvent.callEvent() ? (int) Math.round(pointEvent.getAmount()) : 0);
    }

}
