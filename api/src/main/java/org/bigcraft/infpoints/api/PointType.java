package org.bigcraft.infpoints.api;

import java.util.UUID;

public interface PointType {

    double get(UUID player);
    void add(UUID player, double amount);
    boolean subtract(UUID player, double amount);
    void set(UUID player, double amount);
    boolean transfer(UUID sender, UUID receiver, double amount);

}
