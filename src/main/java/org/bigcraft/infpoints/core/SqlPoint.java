package org.bigcraft.infpoints.core;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.j256.ormlite.table.TableUtils;
import org.bigcraft.infpoints.InfPoints;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SqlPoint extends Point implements AutoCloseable {

    private final MemoryPoint pointCache;
    private Dao<PointEntity, UUID> pointDao;

    private final ExecutorService executor;

    public SqlPoint(ConfigurationSection config, ConnectionSource connectionSource) {
        super(config);
        pointCache = new MemoryPoint(this);
        executor = Executors.newCachedThreadPool();
        try {
            DatabaseTableConfig<PointEntity> tableConfig = DatabaseTableConfig.fromClass(connectionSource.getDatabaseType(), PointEntity.class);
            tableConfig.setTableName(getConfig().key());
            this.pointDao = new PointEntityDao(connectionSource, tableConfig);
            if (!pointDao.isTableExists())
                TableUtils.createTable(pointDao);
            pointDao.queryForAll().forEach(pointEntity ->
                    pointCache.set(pointEntity.getPlayer(), pointEntity.getBalance()));
        } catch (SQLException e) {
            InfPoints.getInstance().getLog().error("An exception occurred while creating '{}' table", getConfig().key(), e);
        }
    }

    @Override
    public double get(UUID player) {
        return pointCache.get(player);
    }

    @Override
    public void set(UUID player, double amount) {
        pointCache.set(player, amount);
        executor.execute(() -> {
            getEntity(player)
                    .ifPresentOrElse(entity -> update(entity, amount),
                            () -> create(player, amount));
        });
    }

    private Optional<PointEntity> getEntity(UUID uuid) {
        try {
            return Optional.ofNullable(pointDao.queryForId(uuid));
        } catch (SQLException e) {
            InfPoints.getInstance().getLog().error("An exception occurred while querying point entity", e);
        }
        return Optional.empty();
    }

    private void create(UUID player, double balance) {
        try {
            pointDao.create(new PointEntity(player, balance));
        } catch (SQLException e) {
            InfPoints.getInstance().getLog().error("An exception occurred while creating point entity", e);
        }
    }

    private void update(PointEntity entity, double balance) {
        try {
            pointDao.update(entity.setBalance(balance));
        } catch (SQLException e) {
            InfPoints.getInstance().getLog().error("An exception occurred while updating point entity", e);
        }
    }

    @Override
    public void close() {
        executor.shutdown();
    }

}
