package org.bigcraft.infpoints.storage;

import org.bigcraft.infpoints.api.PointStorageType;
import org.bigcraft.infpoints.api.transaction.TransactionResult.Status;
import org.bigcraft.infpoints.api.transaction.TransactionType;
import org.bigcraft.infpoints.point.PointDefinition;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.bigcraft.infpoints.point.TestPoints.add;
import static org.bigcraft.infpoints.point.TestPoints.definition;
import static org.bigcraft.infpoints.point.TestPoints.set;
import static org.bigcraft.infpoints.point.TestPoints.subtract;
import static org.bigcraft.infpoints.point.TestPoints.transfer;
import static org.bigcraft.infpoints.point.TestPoints.withKey;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MemoryStorageTest {

    private final PointDefinition point = definition("memory", PointStorageType.MEMORY, 10, 2);
    private final MemoryStorage storage = new MemoryStorage();
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    @Test
    void appliesOperationsFromDefaultBalance() {
        assertEquals(1000, storage.balance(point, alice));
        assertEquals(Status.SUCCESS, storage.apply(point, add(alice, 250)).status());
        assertEquals(Status.INSUFFICIENT_FUNDS, storage.apply(point, subtract(alice, 5000)).status());
        StorageResult subtracted = storage.apply(point, subtract(alice, 50));
        assertEquals(1200, subtracted.balance());
        assertEquals(-50, subtracted.entries().get(0).units());
        StorageResult set = storage.apply(point, set(alice, 300));
        assertEquals(TransactionType.SET, set.entries().get(0).type());
        assertEquals(-900, set.entries().get(0).units());
        assertEquals(300, storage.balance(point, alice));
    }

    @Test
    void transferMovesBothBalancesOrNeither() {
        assertEquals(Status.INSUFFICIENT_FUNDS, storage.apply(point, transfer(alice, bob, 1001)).status());
        assertEquals(1000, storage.balance(point, alice));
        assertEquals(1000, storage.balance(point, bob));
        StorageResult result = storage.apply(point, transfer(alice, bob, 400));
        assertEquals(Status.SUCCESS, result.status());
        assertEquals(600, storage.balance(point, alice));
        assertEquals(1400, storage.balance(point, bob));
        assertEquals(result.entries().get(0).correlationId(), result.entries().get(1).correlationId());
    }

    @Test
    void idempotencyKeyAppliesOnce() {
        assertEquals(Status.SUCCESS, storage.apply(point, withKey(add(alice, 100), "order")).status());
        assertEquals(Status.DUPLICATE, storage.apply(point, withKey(add(alice, 100), "order")).status());
        assertEquals(1100, storage.balance(point, alice));
    }

}
