package me.wyne.infpoints.storage.sql;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;

public interface LegacyImports {

    LegacyImports NONE = new LegacyImports() {
        @Override
        public @Nullable Map<String, Double> jsonBalances(@NotNull String point) {
            return null;
        }

        @Override
        public void jsonImported(@NotNull String point) {
        }
    };

    // Balances of a 2.x JSON point keyed by player UUID, or null when there is no such file
    @Nullable Map<String, Double> jsonBalances(@NotNull String point) throws IOException;

    void jsonImported(@NotNull String point);

}
