package org.bigcraft.infpoints.core;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.j256.ormlite.table.TableUtils;
import lombok.SneakyThrows;
import org.bigcraft.infpoints.InfPoints;
import org.bigcraft.infpoints.sql.ConnectionProvider;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SqlPoint extends Point implements AutoCloseable {

    private final MemoryPoint pointCache;
    private Dao<PointEntity, UUID> pointDao;

    private final ExecutorService executor;

    public SqlPoint(ConfigurationSection config, ConnectionProvider connectionProvider) {
        super(config);
        pointCache = new MemoryPoint(this);
        executor = Executors.newSingleThreadExecutor();
        if (connectionProvider.isActive()) {
            try {
                DatabaseTableConfig<PointEntity> tableConfig = DatabaseTableConfig.fromClass(connectionProvider.getConnectionPool().getSource().getDatabaseType(), PointEntity.class);
                tableConfig.setTableName(getConfig().key());
                this.pointDao = new PointEntityDao(connectionProvider.getConnectionPool().getSource(), tableConfig);
                if (!pointDao.isTableExists())
                    TableUtils.createTable(pointDao);
                pointDao.queryForAll().forEach(pointEntity ->
                        pointCache.set(pointEntity.getPlayer(), pointEntity.getBalance()));
            } catch (SQLException e) {
                InfPoints.getInstance().getLog().error("An exception occurred while creating '{}' table", getConfig().key(), e);
            }
        } else {
            InfPoints.getInstance().getLog().error("Couldn't connect to point database, subsequent requests will fail");
        }
    }

    @Override
    public double get(UUID player) {
        return pointCache.get(player);
    }

    @Override
    public void set(UUID player, double amount) {
        pointCache.set(player, amount);
        if (pointDao == null) return;
        executor.execute(() -> {
            try {
                pointDao.createOrUpdate(new PointEntity(player, amount));
            } catch (SQLException e) {
                InfPoints.getInstance().getLog().error("An exception occurred while saving point entity", e);
            }
        });
    }

    @SneakyThrows
    @Override
    public void close() {
        executor.shutdown();
        if (!executor.awaitTermination(60, TimeUnit.SECONDS))
            executor.shutdownNow();
    }

}
