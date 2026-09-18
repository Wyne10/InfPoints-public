package me.wyne.infpoints.storage.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@SuppressWarnings("unused")
@DatabaseTable(tableName = "transaction")
final class TransactionEntity {

    @DatabaseField(columnName = "id", generatedId = true)
    private long id;

    @DatabaseField(columnName = "point_key", canBeNull = false, width = 64, uniqueCombo = true, indexName = "transaction_player_idx")
    private String pointKey;

    @DatabaseField(columnName = "player", canBeNull = false, width = 36, indexName = "transaction_player_idx")
    private String player;

    @DatabaseField(columnName = "tx_type", canBeNull = false, width = 16)
    private String type;

    @DatabaseField(columnName = "amount", canBeNull = false)
    private long amount;

    @DatabaseField(columnName = "balance_after", canBeNull = false)
    private long balanceAfter;

    @DatabaseField(columnName = "correlation_id", width = 36)
    private String correlationId;

    @DatabaseField(columnName = "idempotency_key", width = 128, uniqueCombo = true)
    private String idempotencyKey;

    @DatabaseField(columnName = "source", width = 64)
    private String source;

    @DatabaseField(columnName = "actor", width = 36)
    private String actor;

    @DatabaseField(columnName = "reason", width = 255)
    private String reason;

    @DatabaseField(columnName = "server", width = 64)
    private String server;

    @DatabaseField(columnName = "created_at", canBeNull = false)
    private long createdAt;

}
