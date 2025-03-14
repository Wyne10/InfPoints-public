package org.bigcraft.infpoints.api;

import java.util.UUID;

public interface PointType {

    int get(UUID player);
    void add(UUID player, int amount);
    boolean subtract(UUID player, int amount);
    void set(UUID player, int amount);
    boolean transfer(UUID sender, UUID receiver, int amount);

}
