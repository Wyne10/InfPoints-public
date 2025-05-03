package org.bigcraft.infpoints.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.wyne.wutils.i18n.I18n;
import me.wyne.wutils.i18n.language.replacement.Placeholder;
import org.bigcraft.infpoints.InfPoints;
import org.bigcraft.infpoints.api.event.PointAddEvent;
import org.bigcraft.infpoints.api.event.PointEvent;
import org.bigcraft.infpoints.api.event.PointSetEvent;
import org.bigcraft.infpoints.api.event.PointSubtractEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Singleton
public class PointEventListener implements Listener {

    @Inject
    public PointEventListener(InfPoints plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    private void onBalanceSet(PointSetEvent event) {
        sendMessage("info-balance-set", event);
    }

    @EventHandler(ignoreCancelled = true)
    private void onBalanceAdd(PointAddEvent event) {
        sendMessage("info-balance-add", event);
    }

    @EventHandler(ignoreCancelled = true)
    private void onBalanceSubtract(PointSubtractEvent event) {
        sendMessage("info-balance-sub", event);
    }

    private void sendMessage(String message, PointEvent event) {
        Player player = Bukkit.getPlayer(event.getPlayer());
        if (player != null) {
            I18n.global.getPlaceholderComponent(player.locale(), player, message,
                    Placeholder.replace("key", event.getPointConfig().getConfig().key()),
                    Placeholder.replace("amount", event.getAmount())
            ).sendMessage(player);
        }
    }

}
