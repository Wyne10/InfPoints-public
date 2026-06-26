package org.bigcraft.infpoints.core.factory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bigcraft.infpoints.InfPoints;
import org.bigcraft.infpoints.core.MemoryPoint;
import org.bigcraft.infpoints.core.Point;
import org.bigcraft.infpoints.core.SqlPoint;
import org.bigcraft.infpoints.sql.ConnectionProvider;
import org.bukkit.configuration.ConfigurationSection;

@Singleton
public class SqlPointFactory implements PointFactory {

    private final ConnectionProvider connectionProvider;

    @Inject
    public SqlPointFactory(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Point create(ConfigurationSection config) {
        if (!connectionProvider.isActive()) {
            InfPoints.getInstance().getLog().warn("SQL connection is not active, '{}' point will be using memory type", config.getName());
            return new MemoryPoint(config);
        }
        return new SqlPoint(config, connectionProvider);
    }

}
