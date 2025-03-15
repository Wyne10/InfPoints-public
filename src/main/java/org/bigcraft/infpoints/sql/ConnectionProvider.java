package org.bigcraft.infpoints.sql;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import lombok.Getter;
import lombok.SneakyThrows;
import me.wyne.wutils.jdbc.ConnectionPool;
import me.wyne.wutils.jdbc.OrmLiteConnectionPool;
import me.wyne.wutils.log.Log;
import org.bigcraft.infpoints.config.SqlConfig;

@Singleton
@Getter
public class ConnectionProvider {

    private ConnectionPool<JdbcPooledConnectionSource> connectionPool;

    private final SqlConfig config;

    @Inject
    public ConnectionProvider(SqlConfig config) {
        this.config = config;
        reloadConnectionPool();
    }

    public void reloadConnectionPool() {
        if (!config.isConfigured()) {
            Log.global.warn("SQL connection is not configured");
            return;
        }
        if (connectionPool != null)
            close();
        this.connectionPool = new OrmLiteConnectionPool(config.getJdbcUrl(), config.getUsername(), config.getPassword());
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
