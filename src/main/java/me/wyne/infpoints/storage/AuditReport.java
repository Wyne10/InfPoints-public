package me.wyne.infpoints.storage;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public record AuditReport(int checked, @NotNull List<Mismatch> mismatches) {

    public record Mismatch(@NotNull UUID player, long balance, long ledger) {}

    public AuditReport {
        mismatches = List.copyOf(mismatches);
    }

}
