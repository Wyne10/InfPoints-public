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
    public int get(UUID player) {
        return getData(player)
                .map(data -> data.getOrDefault(key, PersistentDataType.INTEGER, 0))
                .orElse(0);
    }

    @Override
    public void add(UUID player, int amount) {
        getData(player)
                .ifPresent(data -> set(player, get(player) + amount));
    }

    @Override
    public boolean subtract(UUID player, int amount) {
        if (get(player) < amount)
            return false;
        getData(player)
                .ifPresent(data -> set(player, get(player) - amount));
        return true;
    }

    @Override
    public void set(UUID player, int amount) {
        getData(player)
                .ifPresent(data -> data.set(key, PersistentDataType.INTEGER, amount));
    }

    @Override
    public boolean transfer(UUID sender, UUID receiver, int amount) {
        if (!subtract(sender, amount))
            return false;
        add(receiver, amount);
        return true;
    }

    private Optional<PersistentDataContainer> getData(UUID uuid) {
        return Optional.ofNullable(Bukkit.getPlayer(uuid) != null ? Bukkit.getPlayer(uuid).getPersistentDataContainer() : null);
    }

}
