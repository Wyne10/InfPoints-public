package org.bigcraft.infpoints.core;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;
import java.util.UUID;

public class PdcPoint extends Point {

    private final NamespacedKey key;

    public PdcPoint(ConfigurationSection config) {
        super(config);
        this.key = NamespacedKey.fromString("points:" + config.getName());
    }

    @Override
    public double get(UUID player) {
        return getData(player)
                .map(data -> data.getOrDefault(key, PersistentDataType.DOUBLE, 0D))
                .orElse(getConfig().defaultBalance());
    }

    @Override
    public void set(UUID player, double amount) {
        getData(player)
                .ifPresent(data -> data.set(key, PersistentDataType.DOUBLE, amount));
    }

    private Optional<PersistentDataContainer> getData(UUID uuid) {
        return Optional.ofNullable(Bukkit.getPlayer(uuid) != null ? Bukkit.getPlayer(uuid).getPersistentDataContainer() : null);
    }

}
