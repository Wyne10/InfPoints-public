package org.bigcraft.infpoints.core;

import com.j256.ormlite.field.DatabaseField;
import lombok.Getter;

import java.util.UUID;

@Getter
public class PointEntity {

    @DatabaseField(id = true)
    private UUID player;

    @DatabaseField
    private int balance;

    public PointEntity() {
    }

    public PointEntity(UUID player, int balance) {
        this.player = player;
        this.balance = balance;
    }

    public PointEntity setBalance(int balance) {
        this.balance = balance;
        return this;
    }

}
