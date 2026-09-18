package org.bigcraft.infpoints.point;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.wyne.wutils.common.loadable.Loadable;
import me.wyne.wutils.common.loadable.Loader;
import org.bigcraft.infpoints.DefinitionLoader;
import org.bigcraft.infpoints.InfPoints;
import org.bigcraft.infpoints.api.Point;
import org.bigcraft.infpoints.api.PointProvider;
import org.bigcraft.infpoints.api.PointStorageType;
import org.bigcraft.infpoints.storage.StorageFactory;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public final class PointManager implements PointProvider, Loadable {

    private static final List<String> EXAMPLES = List.of("primary", "level", "exp");

    private final InfPoints plugin;
    private final StorageFactory storages;
    private final TransactionExecutor executor;
    private final BalanceCache cache;
    private final DefinitionLoader<PointDefinition> definitions;
    // Handles are never discarded, so references held by other plugins keep working after a reload
    private final Map<String, PointHandle> allHandles = new ConcurrentHashMap<>();
    private volatile Map<String, PointHandle> handles = Map.of();

    @Inject
    public PointManager(InfPoints plugin, StorageFactory storages, TransactionExecutor executor, BalanceCache cache) {
        this.plugin = plugin;
        this.storages = storages;
        this.executor = executor;
        this.cache = cache;
        this.definitions = new DefinitionLoader<>("point", new File(plugin.getDataFolder(), "points"), new PointDefinition.Factory());
        Loader.global.registerLoadable(this);
    }

    @Override
    public void load(@NotNull ConfigurationSection config) {
        File directory = definitions.getDirectory();
        if (!directory.exists()) {
            directory.mkdirs();
            // Only written on a fresh install, so removed examples don't come back on the next load
            if (!config.isConfigurationSection("point"))
                EXAMPLES.forEach(example -> plugin.saveResource("points/" + example + ".yml", false));
        }
        Map<String, PointHandle> active = new LinkedHashMap<>();
        for (PointDefinition definition : definitions.load(config).values()) {
            PointHandle existing = allHandles.get(definition.key());
            PointState previous = existing == null ? null : existing.state();
            PointState state = storages.create(definition, previous);
            if (!state.available())
                InfPoints.logger().error("Point '{}' is unavailable: {}", definition.key(), state.unavailableReason());
            if (previous == null || previous.definition().type() != definition.type() || previous.definition().decimals() != definition.decimals())
                cache.invalidate(definition.key());
            PointHandle handle = existing;
            if (handle == null) {
                handle = new PointHandle(definition.key(), state, executor, cache);
                allHandles.put(definition.key(), handle);
            } else {
                handle.update(state);
            }
            active.put(definition.key(), handle);
        }
        for (PointHandle handle : allHandles.values()) {
            if (active.containsKey(handle.getKey()) || !handle.state().available())
                continue;
            InfPoints.logger().warn("Point '{}' was removed from the configuration, operations on it will fail", handle.getKey());
            handle.update(handle.state().retire("point '" + handle.getKey() + "' is no longer configured"));
            cache.invalidate(handle.getKey());
        }
        handles = Collections.unmodifiableMap(active);
        InfPoints.logger().info("Loaded {} points: {}", active.size(), String.join(", ", active.keySet()));
        if (cache.isEnabled() && cache.isPreloadEnabled())
            active.values().stream().filter(handle -> cache.isEmpty(handle.getKey())).forEach(PointHandle::preloadAll);
    }

    @Override
    public @NotNull Set<String> getKeys() {
        return handles.keySet();
    }

    @Override
    public @Nullable Point getPoint(@NotNull String key) {
        return handles.get(key);
    }

    @Override
    public @NotNull Collection<Point> getPoints() {
        return List.copyOf(handles.values());
    }

    public @Nullable PointHandle getHandle(@NotNull String key) {
        return handles.get(key);
    }

    public @NotNull Collection<PointHandle> getHandles() {
        return handles.values();
    }

    public @Nullable PointHandle firstOfType(@NotNull PointStorageType type) {
        return handles.values().stream()
                .filter(handle -> handle.getConfig().type() == type)
                .findFirst()
                .orElse(null);
    }

}
