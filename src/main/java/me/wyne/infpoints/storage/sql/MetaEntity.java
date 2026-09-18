package me.wyne.infpoints.storage.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@SuppressWarnings("unused")
@DatabaseTable(tableName = "meta")
final class MetaEntity {

    @DatabaseField(columnName = "meta_key", id = true, width = 128)
    private String key;

    @DatabaseField(columnName = "meta_value", width = 255)
    private String value;

}
