package org.bigcraft.infpoints.point;

import org.bigcraft.infpoints.InfPoints;
import org.bigcraft.infpoints.Messages;
import org.bigcraft.infpoints.api.AsyncPoint;
import org.bigcraft.infpoints.api.Point;
import org.bigcraft.infpoints.api.config.CommandConfig;
import org.bigcraft.infpoints.api.config.PointConfig;
import org.bigcraft.infpoints.api.config.VisualConfig;
import org.bigcraft.infpoints.api.event.PointAddEvent;
import org.bigcraft.infpoints.api.event.PointEvent;
import org.bigcraft.infpoints.api.event.PointSetEvent;
import org.bigcraft.infpoints.api.event.PointSubtractEvent;
import org.bigcraft.infpoints.api.event.PointTransactionCompleteEvent;
import org.bigcraft.infpoints.api.event.PointTransferEvent;
import org.bigcraft.infpoints.api.transaction.Transaction;
import org.bigcraft.infpoints.api.transaction.TransactionRequest;
import org.bigcraft.infpoints.api.transaction.TransactionRequest.Operation;
import org.bigcraft.infpoints.api.transaction.TransactionResult;
import org.bigcraft.infpoints.api.transaction.TransactionResult.Status;
import org.bigcraft.infpoints.storage.AuditReport;
import org.bigcraft.infpoints.storage.LedgerEntry;
import org.bigcraft.infpoints.storage.Mutation;
import org.bigcraft.infpoints.storage.PlayerOfflineException;
import org.bigcraft.infpoints.storage.PointStorage;
import org.bigcraft.infpoints.storage.SqlStorage;
import org.bigcraft.infpoints.storage.StorageException;
import org.bigcraft.infpoints.storage.StorageResult;
import org.bigcraft.infpoints.storage.StorageUnavailableException;
import org.bigcraft.infpoints.storage.ThreadPolicy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class PointHandle implements Point {

    private static final UUID AUDIT_KEY = new UUID(0, 0);
    private static final int PRELOAD_PAGE_SIZE = 1000;
    private static final long UNAVAILABLE_WARNING_INTERVAL = TimeUnit.SECONDS.toNanos(10);

    private final String key;
    private final TransactionExecutor executor;
    private final BalanceCache cache;
    private final AsyncPoint async = new AsyncHandle();
    private volatile PointState state;
    private volatile long lastUnavailableWarning;

    PointHandle(@NotNull String key, @NotNull PointState state, @NotNull TransactionExecutor executor, @NotNull BalanceCache cache) {
        this.key = key;
        this.state = state;
        this.executor = executor;
        this.cache = cache;
    }

    public @NotNull PointState state() {
        return state;
    }

    void update(@NotNull PointState state) {
        this.state = state;
    }

    @Override
    public @NotNull String getKey() {
        return key;
    }

    @Override
    public @NotNull PointConfig getConfig() {
        return state.definition().config();
    }

    @Override
    public @NotNull VisualConfig getVisualConfig() {
        return state.definition().visualConfig();
    }

    @Override
    public @NotNull CommandConfig getCommandConfig() {
        return state.definition().commandConfig();
    }

    @Override
    public boolean isAvailable() {
        return state.available();
    }

    @Override
    public boolean supportsHistory() {
        return state.storage().supportsHistory();
    }

    @Override
    public boolean supportsOfflinePlayers() {
        return state.storage().supportsOfflinePlayers();
    }

    @Override
    public double get(@NotNull UUID player) {
        PointState state = this.state;
        PointStorage storage = state.storage();
        if (storage.threadPolicy() == ThreadPolicy.MAIN && !Bukkit.isPrimaryThread())
            return executor.callOnMainThread(() -> read(this.state, player), () -> lastKnown(player));
        if (storage.isRemote() && cache.isEnabled()) {
            Optional<BalanceCache.Entry> cached = cache.lookup(key, player);
            if (cached.isPresent()) {
                if (cache.isStale(cached.get()))
                    refresh(state, player);
                return cached.get().balance();
            }
        }
        return read(state, player);
    }

    @Override
    public @NotNull OptionalDouble getCached(@NotNull UUID player) {
        PointState state = this.state;
        PointStorage storage = state.storage();
        boolean readable = !storage.isRemote() && (storage.threadPolicy() == ThreadPolicy.ANY || Bukkit.isPrimaryThread());
        if (readable && state.available())
            return OptionalDouble.of(read(state, player));
        Optional<BalanceCache.Entry> cached = cache.lookup(key, player);
        if (cached.isEmpty() || cache.isStale(cached.get()))
            refresh(state, player);
        return cached.map(entry -> OptionalDouble.of(entry.balance())).orElse(OptionalDouble.empty());
    }

    @Override
    public @NotNull String getFormat(@NotNull UUID player) {
        return Messages.format(this, get(player));
    }

    @Override
    public boolean add(@NotNull UUID player, double amount) {
        return execute(TransactionRequest.add(player, amount)).isApplied();
    }

    @Override
    public boolean subtract(@NotNull UUID player, double amount) {
        return execute(TransactionRequest.subtract(player, amount)).isApplied();
    }

    @Override
    public boolean set(@NotNull UUID player, double amount) {
        return execute(TransactionRequest.set(player, amount)).isApplied();
    }

    @Override
    public boolean transfer(@NotNull UUID sender, @NotNull UUID receiver, double amount) {
        if (sender.equals(receiver))
            return false;
        return execute(TransactionRequest.transfer(sender, receiver, amount)).isApplied();
    }

    @Override
    public @NotNull TransactionResult execute(@NotNull TransactionRequest request) {
        TransactionRequest resolved = withSource(request);
        if (state.storage().threadPolicy() == ThreadPolicy.MAIN && !Bukkit.isPrimaryThread())
            return executor.callOnMainThread(() -> apply(resolved), () -> failure(Status.UNAVAILABLE, resolved));
        return apply(resolved);
    }

    @Override
    public boolean add(@NotNull Player player, double amount) {
        TransactionResult result = execute(TransactionRequest.add(player.getUniqueId(), amount));
        if (result.status() == Status.SUCCESS)
            Messages.send(player, "info-balance-add", Messages.key(this), Messages.amount(this, result.amount()));
        return result.isApplied();
    }

    @Override
    public boolean subtract(@NotNull Player player, double amount) {
        TransactionResult result = execute(TransactionRequest.subtract(player.getUniqueId(), amount));
        if (result.status() == Status.SUCCESS)
            Messages.send(player, "info-balance-sub", Messages.key(this), Messages.amount(this, result.amount()));
        else if (result.status() == Status.INSUFFICIENT_FUNDS)
            Messages.send(player, "error-insufficient-funds", Messages.key(this));
        return result.isApplied();
    }

    @Override
    public boolean set(@NotNull Player player, double amount) {
        TransactionResult result = execute(TransactionRequest.set(player.getUniqueId(), amount));
        if (result.status() == Status.SUCCESS)
            Messages.send(player, "info-balance-set", Messages.key(this), Messages.amount(this, result.amount()));
        return result.isApplied();
    }

    @Override
    public boolean transfer(@NotNull Player sender, @NotNull Player receiver, double amount) {
        if (sender.getUniqueId().equals(receiver.getUniqueId()))
            return false;
        TransactionResult result = execute(TransactionRequest.transfer(sender.getUniqueId(), receiver.getUniqueId(), amount));
        if (result.status() == Status.SUCCESS) {
            Messages.sendAbout(sender, receiver, "success-point-pay", Messages.key(this), Messages.amount(this, result.amount()), Messages.playerName(receiver));
            Messages.sendAbout(receiver, sender, "info-point-receive", Messages.key(this), Messages.amount(this, result.amount()), Messages.playerName(sender));
        } else if (result.status() == Status.INSUFFICIENT_FUNDS) {
            Messages.send(sender, "error-insufficient-funds", Messages.key(this));
        }
        return result.isApplied();
    }

    @Override
    public @NotNull AsyncPoint async() {
        return async;
    }

    public void preloadAll() {
        PointState state = this.state;
        if (state.available() && state.storage() instanceof SqlStorage storage)
            preloadPage(state, storage, 0, 0);
    }

    // Each page is its own job, so warming up a large point doesn't hold up a worker's queue
    private void preloadPage(PointState state, SqlStorage storage, int offset, int loaded) {
        executor.submit(UUID.randomUUID(), "preload of balances of point '" + key + "'", () -> {
            PointDefinition definition = state.definition();
            Map<UUID, Long> page;
            try {
                page = storage.balances(definition, offset, PRELOAD_PAGE_SIZE);
            } catch (StorageException e) {
                InfPoints.logger().warn("Couldn't read the balances of point '{}' into the cache, they are read when they are used: {}",
                        key, e.getMessage());
                return loaded;
            }
            page.forEach((player, units) -> cache.putIfAbsent(key, player, definition.toAmount(units)));
            int total = loaded + page.size();
            if (page.size() < PRELOAD_PAGE_SIZE || state != this.state) {
                InfPoints.logger().info("Read {} balances of point '{}' into the cache", total, key);
                return total;
            }
            preloadPage(state, storage, offset + page.size(), total);
            return total;
        }, () -> loaded);
    }

    public void preload(@NotNull UUID player) {
        PointState state = this.state;
        if (state.storage().isRemote() && state.available())
            read(state, player);
    }

    public @NotNull CompletableFuture<Optional<AuditReport>> audit(@Nullable UUID player) {
        return executor.submit(player != null ? player : AUDIT_KEY, "audit of point '" + key + "'", () -> {
            PointState state = this.state;
            if (!state.available() || !(state.storage() instanceof SqlStorage storage))
                return Optional.empty();
            try {
                return Optional.of(storage.audit(state.definition(), player));
            } catch (StorageException e) {
                InfPoints.logger().error("Audit of point '{}' failed", key, e);
                return Optional.empty();
            }
        }, Optional::empty);
    }

    private TransactionResult apply(TransactionRequest request) {
        PointState state = this.state;
        if (!state.available()) {
            warnUnavailable(request, state.unavailableReason());
            return failure(Status.UNAVAILABLE, request);
        }
        PointDefinition definition = state.definition();
        PointStorage storage = state.storage();
        if (storage.threadPolicy() == ThreadPolicy.MAIN && !Bukkit.isPrimaryThread())
            return executor.callOnMainThread(() -> apply(request), () -> failure(Status.UNAVAILABLE, request));
        if (isInvalidAmount(request, definition))
            return failure(Status.INVALID_AMOUNT, request);
        if (!storage.supportsOfflinePlayers() && (!isOnline(request.player()) || (request.receiver() != null && !isOnline(request.receiver()))))
            return failure(Status.PLAYER_OFFLINE, request);
        if (request.idempotencyKey() != null && !storage.supportsIdempotency()) {
            InfPoints.logger().error("Point '{}' can't apply requests with an idempotency key, {} was not applied", key, request);
            return failure(Status.FAILED, request);
        }

        PointEvent event = createEvent(request);
        if (!event.callEvent())
            return failure(Status.CANCELLED, request);
        TransactionRequest effective = event.getRequest();
        if (isInvalidAmount(effective, definition))
            return failure(Status.INVALID_AMOUNT, effective);

        Mutation mutation = Mutation.of(effective, definition.toUnits(effective.amount()));
        StorageResult stored;
        try {
            stored = storage.apply(definition, mutation);
        } catch (PlayerOfflineException e) {
            return failure(Status.PLAYER_OFFLINE, effective);
        } catch (StorageUnavailableException e) {
            warnUnavailable(effective, e.getMessage());
            return failure(Status.UNAVAILABLE, effective);
        } catch (StorageException e) {
            InfPoints.logger().error("Point '{}' couldn't apply {}, nothing was changed unless the database lost the connection while committing", key, effective, e);
            return failure(Status.FAILED, effective);
        }

        remember(definition, mutation, stored);
        TransactionResult result = toResult(definition, mutation, stored);
        if (result.status() == Status.FAILED)
            InfPoints.logger().error("Point '{}' refused {}, the balance would overflow", key, effective);
        if (result.status() == Status.SUCCESS)
            executor.runOnMainThread(() -> new PointTransactionCompleteEvent(this, effective, result).callEvent());
        return result;
    }

    private double read(PointState state, UUID player) {
        if (!state.available())
            return lastKnown(player);
        PointDefinition definition = state.definition();
        try {
            double balance = definition.toAmount(state.storage().balance(definition, player));
            cache.put(key, player, balance);
            return balance;
        } catch (PlayerOfflineException e) {
            return lastKnown(player);
        } catch (StorageUnavailableException e) {
            InfPoints.logger().debug("Couldn't read a balance of point '{}': {}", key, e.getMessage());
            return lastKnown(player);
        } catch (StorageException e) {
            InfPoints.logger().error("Couldn't read a balance of point '{}'", key, e);
            return lastKnown(player);
        }
    }

    private double lastKnown(UUID player) {
        return cache.peek(key, player).orElse(0);
    }

    private void refresh(PointState state, UUID player) {
        if (!state.available() || !cache.startRefresh(key, player))
            return;
        Supplier<Double> task = () -> {
            try {
                return read(this.state, player);
            } finally {
                cache.finishRefresh(key, player);
            }
        };
        Supplier<Double> fallback = () -> {
            cache.finishRefresh(key, player);
            return lastKnown(player);
        };
        if (state.storage().threadPolicy() == ThreadPolicy.MAIN)
            executor.supplyOnMainThread(task, fallback);
        else
            executor.submit(player, "refresh of a balance of point '" + key + "'", task, fallback);
    }

    private List<Transaction> readHistory(UUID player, int offset, int limit) {
        PointState state = this.state;
        if (!state.available() || !state.storage().supportsHistory())
            return List.of();
        try {
            return state.storage().history(state.definition(), player, offset, limit).stream()
                    .map(entry -> toTransaction(state.definition(), entry))
                    .toList();
        } catch (StorageException e) {
            InfPoints.logger().error("Couldn't read the history of point '{}'", key, e);
            return List.of();
        }
    }

    private Optional<Transaction> readTransaction(long id) {
        PointState state = this.state;
        if (!state.available() || !state.storage().supportsHistory())
            return Optional.empty();
        try {
            return state.storage().transaction(state.definition(), id).map(entry -> toTransaction(state.definition(), entry));
        } catch (StorageException e) {
            InfPoints.logger().error("Couldn't read a transaction of point '{}'", key, e);
            return Optional.empty();
        }
    }

    private TransactionRequest withSource(TransactionRequest request) {
        if (request.source() != null)
            return request;
        String caller = CallerResolver.callingPlugin();
        return caller == null ? request : request.withSource(caller);
    }

    private PointEvent createEvent(TransactionRequest request) {
        return switch (request.operation()) {
            case ADD -> new PointAddEvent(this, request);
            case SUBTRACT -> new PointSubtractEvent(this, request);
            case SET -> new PointSetEvent(this, request);
            case TRANSFER -> new PointTransferEvent(this, request);
        };
    }

    private void remember(PointDefinition definition, Mutation mutation, StorageResult stored) {
        if (stored.status() == Status.SUCCESS) {
            for (LedgerEntry entry : stored.entries())
                cache.put(key, entry.player(), definition.toAmount(entry.balance()));
        } else if (stored.status() != Status.DUPLICATE && stored.hasBalance()) {
            cache.put(key, mutation.player(), definition.toAmount(stored.balance()));
        }
    }

    private void warnUnavailable(TransactionRequest request, @Nullable String reason) {
        long now = System.nanoTime();
        if (lastUnavailableWarning != 0 && now - lastUnavailableWarning < UNAVAILABLE_WARNING_INTERVAL)
            return;
        lastUnavailableWarning = now;
        InfPoints.logger().warn("Point '{}' is unavailable, {} was not applied: {}", key, request, reason);
    }

    private static boolean isInvalidAmount(TransactionRequest request, PointDefinition definition) {
        try {
            long units = definition.toUnits(request.amount());
            return request.operation() == Operation.SET ? units < 0 : units <= 0;
        } catch (ArithmeticException e) {
            return true;
        }
    }

    private static boolean isOnline(UUID player) {
        return Bukkit.getPlayer(player) != null;
    }

    private static TransactionResult failure(Status status, TransactionRequest request) {
        return new TransactionResult(status, request.amount(), Double.NaN, List.of());
    }

    private static TransactionResult toResult(PointDefinition definition, Mutation mutation, StorageResult stored) {
        double balance = stored.hasBalance() ? definition.toAmount(stored.balance()) : Double.NaN;
        List<Transaction> transactions = stored.entries().stream()
                .map(entry -> toTransaction(definition, entry))
                .toList();
        return new TransactionResult(stored.status(), definition.toAmount(mutation.units()), balance, transactions);
    }

    private static Transaction toTransaction(PointDefinition definition, LedgerEntry entry) {
        return new Transaction(entry.id(), entry.point(), entry.player(), entry.type(), definition.toAmount(entry.units()),
                definition.toAmount(entry.balance()), entry.correlationId(), entry.idempotencyKey(), entry.source(), entry.actor(),
                entry.reason(), entry.server(), entry.createdAt());
    }

    private final class AsyncHandle implements AsyncPoint {

        @Override
        public @NotNull CompletableFuture<Double> get(@NotNull UUID player) {
            if (state.storage().threadPolicy() == ThreadPolicy.MAIN)
                return executor.supplyOnMainThread(() -> read(state, player), () -> lastKnown(player));
            return executor.submit(player, "read of a balance of point '" + key + "'", () -> read(state, player), () -> lastKnown(player));
        }

        @Override
        public @NotNull CompletableFuture<Boolean> add(@NotNull UUID player, double amount) {
            return execute(TransactionRequest.add(player, amount)).thenApply(TransactionResult::isApplied);
        }

        @Override
        public @NotNull CompletableFuture<Boolean> subtract(@NotNull UUID player, double amount) {
            return execute(TransactionRequest.subtract(player, amount)).thenApply(TransactionResult::isApplied);
        }

        @Override
        public @NotNull CompletableFuture<Boolean> set(@NotNull UUID player, double amount) {
            return execute(TransactionRequest.set(player, amount)).thenApply(TransactionResult::isApplied);
        }

        @Override
        public @NotNull CompletableFuture<Boolean> transfer(@NotNull UUID sender, @NotNull UUID receiver, double amount) {
            if (sender.equals(receiver))
                return CompletableFuture.completedFuture(false);
            return execute(TransactionRequest.transfer(sender, receiver, amount)).thenApply(TransactionResult::isApplied);
        }

        @Override
        public @NotNull CompletableFuture<TransactionResult> execute(@NotNull TransactionRequest request) {
            TransactionRequest resolved = withSource(request);
            if (state.storage().threadPolicy() == ThreadPolicy.MAIN)
                return executor.supplyOnMainThread(() -> apply(resolved), () -> failure(Status.UNAVAILABLE, resolved));
            return executor.submit(resolved.player(), resolved, () -> apply(resolved), () -> failure(Status.UNAVAILABLE, resolved));
        }

        @Override
        public @NotNull CompletableFuture<List<Transaction>> history(@NotNull UUID player, int offset, int limit) {
            return executor.submit(player, "history of point '" + key + "'", () -> readHistory(player, offset, limit), List::of);
        }

        @Override
        public @NotNull CompletableFuture<Optional<Transaction>> transaction(long id) {
            return executor.submit(new UUID(0, id), "transaction #" + id + " of point '" + key + "'", () -> readTransaction(id), Optional::empty);
        }

    }

}
