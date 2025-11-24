package org.bigcraft.infpoints.api;

import org.bukkit.entity.Player;

public interface PointView {

    void add(Player player, double amount);
    boolean subtract(Player player, double amount);
    void set(Player player, double amount);
    boolean transfer(Player sender, Player receiver, double amount);

}
