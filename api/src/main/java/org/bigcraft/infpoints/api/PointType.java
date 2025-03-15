package org.bigcraft.infpoints.api;

import java.util.UUID;

public interface PointType {

    long get(UUID player);
    void add(UUID player, long amount);
    boolean subtract(UUID player, long amount);
    void set(UUID player, long amount);
    boolean transfer(UUID sender, UUID receiver, long amount);

}
