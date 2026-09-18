package me.wyne.infpoints.storage.sql;

import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

final class LedgerSession implements AutoCloseable {

    private final ConnectionSource source;
    private final DatabaseConnection databaseConnection;
    private final Connection connection;
    private final int queryTimeoutSeconds;
    private final boolean autoCommit;
    private final int networkTimeout;

    LedgerSession(ConnectionSource source, DatabaseConnection databaseConnection, int queryTimeoutSeconds) throws SQLException {
        this.source = source;
        this.databaseConnection = databaseConnection;
        this.connection = databaseConnection.getUnderlyingConnection();
        this.queryTimeoutSeconds = Math.max(0, queryTimeoutSeconds);
        this.autoCommit = connection.getAutoCommit();
        this.networkTimeout = applyNetworkTimeout(connection, this.queryTimeoutSeconds);
        connection.setAutoCommit(false);
    }

    // A statement timeout can't interrupt a socket read that never returns, e.g. while committing,
    // so the connection gets a network timeout slightly longer than the statement timeout where supported
    private static int applyNetworkTimeout(Connection connection, int queryTimeoutSeconds) {
        if (queryTimeoutSeconds == 0)
            return -1;
        try {
            int previous = connection.getNetworkTimeout();
            connection.setNetworkTimeout(Runnable::run, (int) TimeUnit.SECONDS.toMillis(queryTimeoutSeconds + 5L));
            return previous;
        } catch (SQLException | AbstractMethodError | UnsupportedOperationException e) {
            return -1;
        }
    }

    Connection connection() {
        return connection;
    }

    PreparedStatement prepare(String sql) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setQueryTimeout(queryTimeoutSeconds);
        return statement;
    }

    PreparedStatement prepareInsert(String sql) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        statement.setQueryTimeout(queryTimeoutSeconds);
        return statement;
    }

    void commit() throws SQLException {
        connection.commit();
    }

    void rollback() {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }

    @Override
    public void close() {
        rollback();
        try {
            connection.setAutoCommit(autoCommit);
        } catch (SQLException ignored) {
        }
        if (networkTimeout >= 0) {
            try {
                connection.setNetworkTimeout(Runnable::run, networkTimeout);
            } catch (SQLException | AbstractMethodError | UnsupportedOperationException ignored) {
            }
        }
        try {
            source.releaseConnection(databaseConnection);
        } catch (SQLException ignored) {
        }
    }

}
