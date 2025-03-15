package org.bigcraft.infpoints.core;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.j256.ormlite.table.TableUtils;
import me.wyne.wutils.log.Log;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class SqlPoint extends Point {

    private Dao<PointEntity, UUID> pointDao;

    public SqlPoint(ConfigurationSection config, JdbcPooledConnectionSource connectionSource) {
        super(config);
        try {
            DatabaseTableConfig<PointEntity> tableConfig = DatabaseTableConfig.fromClass(connectionSource.getDatabaseType(), PointEntity.class);
            tableConfig.setTableName(getConfig().key());
            this.pointDao = DaoManager.createDao(connectionSource, tableConfig);
            TableUtils.createTableIfNotExists(connectionSource, tableConfig);
        } catch (SQLException e) {
            Log.global.exception("An exception occurred while creating " + getConfig().key() + " table", e);
        }
    }

    @Override
    public long get(UUID player) {
        return getEntity(player)
                .map(PointEntity::getBalance)
                .orElse(getConfig().defaultBalance());
    }

    @Override
    public void add(UUID player, long amount) {
        getEntity(player)
                .ifPresentOrElse(entity -> update(entity, entity.getBalance() + amount),
                        () -> create(player, getConfig().defaultBalance() + amount));
    }

    @Override
    public boolean subtract(UUID player, long amount) {
        if (get(player) < amount)
            return false;
        getEntity(player)
                .ifPresentOrElse(entity -> update(entity, entity.getBalance() - amount),
                        () -> create(player, getConfig().defaultBalance() - amount));
        return true;
    }

    @Override
    public void set(UUID player, long amount) {
        getEntity(player)
                .ifPresentOrElse(entity -> update(entity, amount),
                        () -> create(player, amount));
    }

    @Override
    public boolean transfer(UUID sender, UUID receiver, long amount) {
        if (!subtract(sender, amount))
            return false;
        add(receiver, amount);
        return true;
    }

    private Optional<PointEntity> getEntity(UUID uuid) {
        try {
            return Optional.ofNullable(pointDao.queryForId(uuid));
        } catch (SQLException e) {
            Log.global.exception("An exception occurred while querying point entity", e);
        }
        return Optional.empty();
    }

    private void create(UUID player, long balance) {
        try {
            pointDao.create(new PointEntity(player, balance));
        } catch (SQLException e) {
            Log.global.exception("An exception occurred while creating the point entity", e);
        }
    }

    private void update(PointEntity entity, long balance) {
        try {
            pointDao.update(entity.setBalance(balance));
        } catch (SQLException e) {
            Log.global.exception("An exception occurred while updating the point entity", e);
        }
    }

}
