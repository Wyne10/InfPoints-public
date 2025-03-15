package org.bigcraft.infpoints.core;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MemoryPoint extends Point {

    private Map<UUID, Long> balance = new HashMap<>();

    public MemoryPoint(ConfigurationSection config) {
        super(config);
    }

    @Override
    public long get(UUID player) {
        return balance.getOrDefault(player, getConfig().defaultBalance());
    }

    @Override
    public void add(UUID player, long amount) {
        set(player, get(player) + amount);
    }

    @Override
    public boolean subtract(UUID player, long amount) {
        if (get(player) < amount)
            return false;
        set(player, get(player) - amount);
        return true;
    }

    @Override
    public void set(UUID player, long amount) {
        balance.put(player, amount);
    }

    @Override
    public boolean transfer(UUID sender, UUID receiver, long amount) {
        if (!subtract(sender, amount))
            return false;
        add(receiver, amount);
        return true;
    }

}
