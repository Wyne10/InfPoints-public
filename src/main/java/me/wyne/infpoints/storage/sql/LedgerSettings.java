package me.wyne.infpoints.storage.sql;

import org.jetbrains.annotations.NotNull;

public record LedgerSettings(@NotNull String tablePrefix, @NotNull String serverName, int queryTimeoutSeconds, int syncWaitMillis) {
}
