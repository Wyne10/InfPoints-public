package org.bigcraft.infpoints.core;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public class XpPoint extends Point {

    public XpPoint(ConfigurationSection config) {
        super(config);
    }

    @Override
    public int get(UUID player) {
        return getPlayer(player)
                .map(Player::getLevel)
                .orElse(0);
    }

    @Override
    public void add(UUID player, int amount) {
        set(player, get(player) + amount);
    }

    @Override
    public boolean subtract(UUID player, int amount) {
        if (get(player) < amount)
            return false;
        set(player, get(player) - amount);
        return true;
    }

    @Override
    public void set(UUID player, int amount) {
        if (amount < 0)
            amount = 0;
        int finalAmount = amount;
        getPlayer(player)
                .ifPresent(player1 -> player1.setLevel(finalAmount));
    }

    @Override
    public boolean transfer(UUID sender, UUID receiver, int amount) {
        if (!subtract(sender, amount))
            return false;
        add(receiver, amount);
        return true;
    }

    private Optional<Player> getPlayer(UUID uuid) {
        return Optional.ofNullable(Bukkit.getPlayer(uuid));
    }

}
