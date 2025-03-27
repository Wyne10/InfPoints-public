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
    public double get(UUID player) {
        return getPlayer(player)
                .map(Player::getLevel)
                .orElse(0);
    }

    @Override
    public void set(UUID player, double amount) {
        if (amount < 0)
            amount = 0;
        int finalAmount = (int) amount;
        getPlayer(player)
                .ifPresent(player1 -> player1.setLevel(finalAmount));
    }

    private Optional<Player> getPlayer(UUID uuid) {
        return Optional.ofNullable(Bukkit.getPlayer(uuid));
    }

}
