package org.bigcraft.infpoints.module;

import com.google.inject.AbstractModule;
import org.bigcraft.connection.api.ConnectionProvider;
import org.bigcraft.infpoints.InfPoints;
import org.bigcraft.infpoints.storage.SqlStorage;
import org.bigcraft.infpoints.storage.sql.SqlLedger;
import org.bigcraft.infpoints.storage.sql.UnavailableSqlStorage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

// Only loaded once ConnectionModule confirmed the ConnectionSource classes exist
final class ConnectionSourceModule extends AbstractModule {

    @Override
    protected void configure() {
        RegisteredServiceProvider<ConnectionProvider> registration = Bukkit.getServicesManager().getRegistration(ConnectionProvider.class);
        if (registration == null) {
            InfPoints.logger().error("ConnectionSource didn't register its connection provider, SQL points are unavailable");
            bind(SqlStorage.class).to(UnavailableSqlStorage.class);
            return;
        }
        bind(ConnectionProvider.class).toInstance(registration.getProvider());
        bind(SqlStorage.class).to(SqlLedger.class);
    }

}
