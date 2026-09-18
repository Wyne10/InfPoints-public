package me.wyne.infpoints.point;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.wyne.wutils.common.scheduler.Schedulers;
import me.wyne.wutils.common.scheduler.Task;
import me.wyne.wutils.common.terminable.Terminable;
import me.wyne.wutils.config.Config;
import me.wyne.wutils.config.ConfigEntry;
import me.wyne.infpoints.InfPoints;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("FieldMayBeFinal")
@Singleton
public final class BalanceCache implements Terminable {

    private static final long CLEANUP_TICKS = 20L * 60;

    @ConfigEntry(section = "Cache", comment = "Seconds after which a cached SQL balance is refreshed in the background, 0 reads the database on every request")
    private int refreshSeconds = 5;

    @ConfigEntry(section = "Cache", comment = "Read every stored balance of SQL points into the cache on load, so balances of offline players are known right away")
    private boolean preload = true;

    @ConfigEntry(section = "Cache", comment = "Seconds an offline player's balance stays cached after it was last used, 0 keeps it until the point is reloaded")
    private int offlineExpireSeconds = 0;

    @ConfigEntry(section = "Cache", comment = "Drop the cached balances of a player when they leave, instead of keeping them for offline lookups")
    private boolean evictOnQuit = false;

    // Balances are kept until they expire, so offline players, leaderboards and placeholders have a value
    private final Map<String, Map<UUID, Entry>> entries = new ConcurrentHashMap<>();
    private final Set<String> refreshing = ConcurrentHashMap.newKeySet();
    private final Task cleanupTask;

    @Inject
    public BalanceCache(InfPoints plugin) {
        Config.global.registerConfigObject(this);
        cleanupTask = Schedulers.async().runRepeating(this::removeExpired, CLEANUP_TICKS, CLEANUP_TICKS);
        plugin.bind(this);
    }

    public static final class Entry {

        private final double balance;
        private final long fetchedAt;
        private volatile long usedAt;

        private Entry(double balance) {
            this.balance = balance;
            this.fetchedAt = System.nanoTime();
            this.usedAt = fetchedAt;
        }

        public double balance() {
            return balance;
        }

    }

    public boolean isEnabled() {
        return refreshSeconds > 0;
    }

    public boolean isPreloadEnabled() {
        return preload;
    }

    public boolean isEvictOnQuit() {
        return evictOnQuit;
    }

    public boolean isEmpty(@NotNull String point) {
        Map<UUID, Entry> balances = entries.get(point);
        return balances == null || balances.isEmpty();
    }

    public @NotNull Optional<Entry> lookup(@NotNull String point, @NotNull UUID player) {
        Map<UUID, Entry> balances = entries.get(point);
        Entry entry = balances == null ? null : balances.get(player);
        if (entry != null)
            entry.usedAt = System.nanoTime();
        return Optional.ofNullable(entry);
    }

    public @NotNull OptionalDouble peek(@NotNull String point, @NotNull UUID player) {
        return lookup(point, player)
                .map(entry -> OptionalDouble.of(entry.balance))
                .orElse(OptionalDouble.empty());
    }

    public boolean isStale(@NotNull Entry entry) {
        return refreshSeconds <= 0 || System.nanoTime() - entry.fetchedAt > TimeUnit.SECONDS.toNanos(refreshSeconds);
    }

    public void put(@NotNull String point, @NotNull UUID player, double balance) {
        entries.computeIfAbsent(point, key -> new ConcurrentHashMap<>()).put(player, new Entry(balance));
    }

    // Preloaded balances never replace one that was read or written while the preload was running
    public void putIfAbsent(@NotNull String point, @NotNull UUID player, double balance) {
        entries.computeIfAbsent(point, key -> new ConcurrentHashMap<>()).putIfAbsent(player, new Entry(balance));
    }

    public boolean startRefresh(@NotNull String point, @NotNull UUID player) {
        return refreshing.add(point + '|' + player);
    }

    public void finishRefresh(@NotNull String point, @NotNull UUID player) {
        refreshing.remove(point + '|' + player);
    }

    public void invalidate(@NotNull String point) {
        entries.remove(point);
    }

    public void evict(@NotNull UUID player) {
        entries.values().forEach(balances -> balances.remove(player));
    }

    @Override
    public void close() {
        cleanupTask.stop();
    }

    private void removeExpired() {
        if (offlineExpireSeconds <= 0)
            return;
        long now = System.nanoTime();
        long expiry = TimeUnit.SECONDS.toNanos(offlineExpireSeconds);
        entries.values().forEach(balances -> balances.entrySet()
                .removeIf(entry -> now - entry.getValue().usedAt > expiry && Bukkit.getPlayer(entry.getKey()) == null));
    }

}
