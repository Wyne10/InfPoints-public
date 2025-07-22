package org.bigcraft.infpoints.core;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MemoryPoint extends Point {

    private Map<UUID, Double> balance = new HashMap<>();

    public MemoryPoint(ConfigurationSection config) {
        super(config);
    }

    public MemoryPoint(Point point) {
        super(point);
    }

    @Override
    public double get(UUID player) {
        return balance.getOrDefault(player, getConfig().defaultBalance());
    }

    @Override
    public void set(UUID player, double amount) {
        balance.put(player, amount);
    }

}
