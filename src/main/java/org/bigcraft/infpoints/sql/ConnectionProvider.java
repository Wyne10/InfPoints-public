package org.bigcraft.infpoints.sql;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.j256.ormlite.support.ConnectionSource;
import lombok.Getter;
import lombok.SneakyThrows;
import me.wyne.wutils.jdbc.ConnectionPool;
import me.wyne.wutils.jdbc.HikariOrmLiteConnectionPool;
import org.bigcraft.infpoints.InfPoints;
import org.bigcraft.infpoints.config.SqlConfig;

import java.sql.SQLException;

@Singleton
@Getter
public class ConnectionProvider {

    private ConnectionPool<ConnectionSource> connectionPool;

    private final SqlConfig config;

    @Inject
    public ConnectionProvider(SqlConfig config) {
        this.config = config;
        reloadConnectionPool();
    }

    public void reloadConnectionPool() {
        if (!config.isConfigured()) {
            InfPoints.getInstance().getLog().warn("SQL connection is not configured");
            return;
        }
        if (connectionPool != null)
            close();
        try {
            this.connectionPool = new HikariOrmLiteConnectionPool(config.getJdbcUrl(), config.getUsername(), config.getPassword());
        } catch (SQLException e) {
            InfPoints.getInstance().getLog().error("An exception occurred trying to establish data source connection with {}", config.getJdbcUrl(), e);
        }
    }

    public boolean isActive() {
        return connectionPool != null && connectionPool.isActive();
    }

    @SneakyThrows
    public void close() {
        if (connectionPool != null)
            connectionPool.close();
    }

}
