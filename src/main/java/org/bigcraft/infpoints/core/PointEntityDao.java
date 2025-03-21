package org.bigcraft.infpoints.core;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;

import java.sql.SQLException;
import java.util.UUID;

public class PointEntityDao extends BaseDaoImpl<PointEntity, UUID> {
    public PointEntityDao(Class<PointEntity> dataClass) throws SQLException {
        super(dataClass);
    }

    public PointEntityDao(ConnectionSource connectionSource, Class<PointEntity> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

    public PointEntityDao(ConnectionSource connectionSource, DatabaseTableConfig<PointEntity> tableConfig) throws SQLException {
        super(connectionSource, tableConfig);
    }
}
