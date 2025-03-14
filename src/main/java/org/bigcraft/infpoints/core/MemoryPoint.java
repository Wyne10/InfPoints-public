package org.bigcraft.infpoints.core;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MemoryPoint extends Point {

    private final Map<UUID, Integer> balance = new HashMap<>();

    public MemoryPoint(ConfigurationSection config) {
        super(config);
    }

    @Override
    public int get(UUID player) {
        return balance.getOrDefault(player, 0);
    }

    @Override
    public void add(UUID player, int amount) {
        balance.put(player, get(player) + amount);
    }

    @Override
    public boolean subtract(UUID player, int amount) {
        if (get(player) < amount)
            return false;
        balance.put(player, get(player) - amount);
        return true;
    }

    @Override
    public void set(UUID player, int amount) {
        balance.put(player, amount);
    }

    @Override
    public boolean transfer(UUID sender, UUID receiver, int amount) {
        if (!subtract(sender, amount))
            return false;
        add(receiver, amount);
        return true;
    }

}
