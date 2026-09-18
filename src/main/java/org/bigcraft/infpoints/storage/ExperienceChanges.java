package org.bigcraft.infpoints.storage;

import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Vanilla reports level changes on the next tick, so levels set by InfPoints are remembered to avoid firing their events twice
@Singleton
public final class ExperienceChanges {

    private final Map<UUID, Integer> expectedLevels = new ConcurrentHashMap<>();

    public void expect(@NotNull UUID player, int level) {
        expectedLevels.put(player, level);
    }

    public boolean consume(@NotNull UUID player, int level) {
        return expectedLevels.remove(player, level);
    }

}
