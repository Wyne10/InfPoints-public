package org.bigcraft.infpoints.point;

import org.bigcraft.infpoints.api.PointStorageType;
import org.bigcraft.infpoints.api.config.CommandConfig;
import org.bigcraft.infpoints.api.config.PointConfig;
import org.bigcraft.infpoints.api.config.VisualConfig;
import org.bigcraft.infpoints.api.transaction.TransactionRequest.Operation;
import org.bigcraft.infpoints.storage.Mutation;

import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;

public final class TestPoints {

    private TestPoints() {}

    public static PointDefinition definition(String key, PointStorageType type, double defaultBalance, int decimals) {
        return new PointDefinition(
                new PointConfig(key, type, defaultBalance, decimals),
                new VisualConfig("Coin", "Coins", "$", "&6", new DecimalFormat("#.##")),
                new CommandConfig(null, List.of(), null, List.of()),
                Amounts.toUnits(defaultBalance, decimals)
        );
    }

    public static Mutation add(UUID player, long units) {
        return new Mutation(Operation.ADD, player, null, units, null, "test", null, null);
    }

    public static Mutation subtract(UUID player, long units) {
        return new Mutation(Operation.SUBTRACT, player, null, units, null, "test", null, null);
    }

    public static Mutation set(UUID player, long units) {
        return new Mutation(Operation.SET, player, null, units, null, "test", null, null);
    }

    public static Mutation transfer(UUID sender, UUID receiver, long units) {
        return new Mutation(Operation.TRANSFER, sender, receiver, units, null, "test", null, null);
    }

    public static Mutation withKey(Mutation mutation, String idempotencyKey) {
        return new Mutation(mutation.operation(), mutation.player(), mutation.receiver(), mutation.units(), mutation.reason(),
                mutation.source(), mutation.actor(), idempotencyKey);
    }

}
