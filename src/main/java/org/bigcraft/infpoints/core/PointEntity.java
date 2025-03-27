package org.bigcraft.infpoints.core;

import com.j256.ormlite.field.DatabaseField;
import lombok.Getter;

import java.util.UUID;

@Getter
public class PointEntity {

    @DatabaseField(id = true)
    private UUID player;

    @DatabaseField
    private double balance;

    public PointEntity() {
    }

    public PointEntity(UUID player, double balance) {
        this.player = player;
        this.balance = balance;
    }

    public PointEntity setBalance(double balance) {
        this.balance = balance;
        return this;
    }

}
