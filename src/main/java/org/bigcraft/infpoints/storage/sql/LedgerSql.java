package org.bigcraft.infpoints.storage.sql;

import com.j256.ormlite.db.DatabaseType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LedgerSql {

    private static final Pattern TOKEN = Pattern.compile("\\{([a-z_]+)}");
    private static final String ENTRY_COLUMNS = "{id}, {point_key}, {player}, {tx_type}, {amount}, {balance_after}, {correlation_id}, " +
            "{idempotency_key}, {source}, {actor}, {reason}, {server}, {created_at}";

    private final DatabaseType databaseType;
    private final String prefix;

    final String accountTable;
    final String transactionTable;
    final String metaTable;

    final String selectBalance;
    final String selectAccounts;
    final String lockAccount;
    final String insertAccount;
    final String addBalance;
    final String subtractBalance;
    final String setBalance;
    final String insertEntry;
    final String selectByIdempotencyKey;
    final String selectByCorrelation;
    final String selectHistory;
    final String selectEntry;
    final String auditCount;
    final String auditCountOfPlayer;
    final String auditMismatches;
    final String auditMismatchesOfPlayer;
    final String selectMeta;
    final String insertMeta;
    final String updateMeta;

    LedgerSql(DatabaseType databaseType, String prefix) {
        this.databaseType = databaseType;
        this.prefix = prefix;
        accountTable = entityName(prefix + "account");
        transactionTable = entityName(prefix + "transaction");
        metaTable = entityName(prefix + "meta");

        selectBalance = sql("SELECT {balance} FROM {account} WHERE {point_key} = ? AND {player} = ?");
        selectAccounts = sql("SELECT {player}, {balance} FROM {account} WHERE {point_key} = ? ORDER BY {player} LIMIT ? OFFSET ?");
        lockAccount = sql("UPDATE {account} SET {version} = {version} + 1, {updated_at} = ? WHERE {point_key} = ? AND {player} = ?");
        insertAccount = sql("INSERT INTO {account} ({point_key}, {player}, {balance}, {version}, {updated_at}) VALUES (?, ?, ?, 0, ?)");
        addBalance = sql("UPDATE {account} SET {balance} = {balance} + ?, {version} = {version} + 1, {updated_at} = ? " +
                "WHERE {point_key} = ? AND {player} = ? AND {balance} <= ?");
        subtractBalance = sql("UPDATE {account} SET {balance} = {balance} - ?, {version} = {version} + 1, {updated_at} = ? " +
                "WHERE {point_key} = ? AND {player} = ? AND {balance} >= ?");
        setBalance = sql("UPDATE {account} SET {balance} = ?, {updated_at} = ? WHERE {point_key} = ? AND {player} = ?");
        insertEntry = sql("INSERT INTO {transaction} ({point_key}, {player}, {tx_type}, {amount}, {balance_after}, {correlation_id}, " +
                "{idempotency_key}, {source}, {actor}, {reason}, {server}, {created_at}) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        selectByIdempotencyKey = sql("SELECT " + ENTRY_COLUMNS + " FROM {transaction} WHERE {point_key} = ? AND {idempotency_key} = ?");
        selectByCorrelation = sql("SELECT " + ENTRY_COLUMNS + " FROM {transaction} WHERE {point_key} = ? AND {correlation_id} = ? ORDER BY {id}");
        selectHistory = sql("SELECT " + ENTRY_COLUMNS + " FROM {transaction} WHERE {point_key} = ? AND {player} = ? ORDER BY {id} DESC LIMIT ? OFFSET ?");
        selectEntry = sql("SELECT " + ENTRY_COLUMNS + " FROM {transaction} WHERE {point_key} = ? AND {id} = ?");
        auditCount = sql("SELECT COUNT(*) FROM {account} WHERE {point_key} = ?");
        auditCountOfPlayer = sql("SELECT COUNT(*) FROM {account} WHERE {point_key} = ? AND {player} = ?");
        String audit = "SELECT a.{player}, a.{balance}, COALESCE(SUM(t.{amount}), 0) FROM {account} a " +
                "LEFT JOIN {transaction} t ON t.{point_key} = a.{point_key} AND t.{player} = a.{player} WHERE a.{point_key} = ?";
        String mismatch = " GROUP BY a.{player}, a.{balance} HAVING a.{balance} <> COALESCE(SUM(t.{amount}), 0)";
        auditMismatches = sql(audit + mismatch);
        auditMismatchesOfPlayer = sql(audit + " AND a.{player} = ?" + mismatch);
        selectMeta = sql("SELECT {meta_value} FROM {meta} WHERE {meta_key} = ?");
        insertMeta = sql("INSERT INTO {meta} ({meta_key}, {meta_value}) VALUES (?, ?)");
        updateMeta = sql("UPDATE {meta} SET {meta_value} = ? WHERE {meta_key} = ?");
    }

    String entityName(String name) {
        return databaseType.isEntityNamesMustBeUpCase() ? databaseType.upCaseEntityName(name) : name;
    }

    String escape(String name) {
        StringBuilder builder = new StringBuilder();
        databaseType.appendEscapedEntityName(builder, entityName(name));
        return builder.toString();
    }

    private String sql(String template) {
        Matcher matcher = TOKEN.matcher(template);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String token = matcher.group(1);
            String name = switch (token) {
                case "account", "transaction", "meta" -> prefix + token;
                default -> token;
            };
            matcher.appendReplacement(builder, Matcher.quoteReplacement(escape(name)));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

}
