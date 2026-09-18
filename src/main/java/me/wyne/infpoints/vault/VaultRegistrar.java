package me.wyne.infpoints.vault;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.wyne.wutils.common.loadable.Loadable;
import me.wyne.wutils.common.loadable.LoadableMeta;
import me.wyne.wutils.common.loadable.Loader;
import me.wyne.wutils.common.terminable.Terminable;
import me.wyne.wutils.config.Config;
import me.wyne.wutils.config.ConfigEntry;
import net.milkbowl.vault.economy.Economy;
import me.wyne.infpoints.InfPoints;
import me.wyne.infpoints.point.PointManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.ServicePriority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("FieldMayBeFinal")
@Singleton
@LoadableMeta(priority = 1)
public final class VaultRegistrar implements Loadable, Terminable {

    @ConfigEntry(section = "Vault", comment = "Provide a point as the Vault economy")
    private boolean enabled = false;

    @ConfigEntry(section = "Vault", comment = "Key of the point provided as the Vault economy")
    private String point = "primary";

    private final InfPoints plugin;
    private final PointManager points;
    private @Nullable VaultEconomy economy;

    @Inject
    public VaultRegistrar(InfPoints plugin, PointManager points) {
        this.plugin = plugin;
        this.points = points;
        Config.global.registerConfigObject(this);
        Loader.global.registerLoadable(this);
        plugin.bind(this);
    }

    @Override
    public void load(@NotNull ConfigurationSection config) {
        if (!enabled) {
            unregister();
            return;
        }
        if (points.getPoint(point) == null)
            InfPoints.logger().error("Point '{}' provided as the Vault economy doesn't exist", point);
        if (economy != null)
            return;
        // The point is looked up on every call, so providers other plugins cached keep following reloads
        economy = new VaultEconomy(() -> points.getPoint(point));
        Bukkit.getServicesManager().register(Economy.class, economy, plugin, ServicePriority.Normal);
        InfPoints.logger().info("Providing point '{}' as the Vault economy", point);
    }

    @Override
    public void close() {
        unregister();
    }

    private void unregister() {
        if (economy == null)
            return;
        Bukkit.getServicesManager().unregister(Economy.class, economy);
        economy = null;
    }

}
