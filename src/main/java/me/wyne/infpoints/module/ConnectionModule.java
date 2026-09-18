package me.wyne.infpoints.module;

import com.google.inject.AbstractModule;
import me.wyne.infpoints.InfPoints;
import me.wyne.infpoints.storage.SqlStorage;
import me.wyne.infpoints.storage.sql.UnavailableSqlStorage;

public final class ConnectionModule extends AbstractModule {

    @Override
    protected void configure() {
        if (isConnectionSourcePresent()) {
            install(new ConnectionSourceModule());
            return;
        }
        InfPoints.logger().warn("ConnectionSource not found, SQL points are unavailable");
        bind(SqlStorage.class).to(UnavailableSqlStorage.class);
    }

    private static boolean isConnectionSourcePresent() {
        try {
            Class.forName("me.wyne.connection.api.ConnectionProvider");
            Class.forName("com.j256.ormlite.support.ConnectionSource");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}
