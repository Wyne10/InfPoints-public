package org.bigcraft.infpoints.storage.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

// Describes the table for ORMLite's schema generation only, rows are read and written with plain JDBC
@SuppressWarnings("unused")
@DatabaseTable(tableName = "account")
final class AccountEntity {

    @DatabaseField(columnName = "id", generatedId = true)
    private long id;

    @DatabaseField(columnName = "point_key", canBeNull = false, width = 64, uniqueCombo = true)
    private String pointKey;

    @DatabaseField(columnName = "player", canBeNull = false, width = 36, uniqueCombo = true)
    private String player;

    @DatabaseField(columnName = "balance", canBeNull = false)
    private long balance;

    @DatabaseField(columnName = "version", canBeNull = false)
    private long version;

    @DatabaseField(columnName = "updated_at", canBeNull = false)
    private long updatedAt;

}
