package org.bigcraft.infpoints.storage.sql;

import org.bigcraft.infpoints.storage.StorageUnavailableException;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

final class CircuitBreaker {

    private final int threshold;
    private final long cooldownNanos;
    private final AtomicInteger failures = new AtomicInteger();
    private volatile boolean open;
    private volatile long openUntil;

    CircuitBreaker(int threshold, Duration cooldown) {
        this.threshold = threshold;
        this.cooldownNanos = cooldown.toNanos();
    }

    void check() throws StorageUnavailableException {
        if (open && System.nanoTime() - openUntil < 0)
            throw new StorageUnavailableException("Database is unreachable, access is paused for a few seconds");
    }

    void recordSuccess() {
        failures.set(0);
        open = false;
    }

    // Returns true when this failure opened the circuit
    boolean recordFailure() {
        if (failures.incrementAndGet() < threshold)
            return false;
        failures.set(0);
        openUntil = System.nanoTime() + cooldownNanos;
        open = true;
        return true;
    }

    long cooldownSeconds() {
        return Duration.ofNanos(cooldownNanos).toSeconds();
    }

}
