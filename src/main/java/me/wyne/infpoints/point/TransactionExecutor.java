package me.wyne.infpoints.point;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.wyne.wutils.common.scheduler.Schedulers;
import me.wyne.wutils.common.terminable.Terminable;
import me.wyne.wutils.config.Config;
import me.wyne.wutils.config.ConfigEntry;
import me.wyne.infpoints.InfPoints;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@SuppressWarnings("FieldMayBeFinal")
@Singleton
public final class TransactionExecutor implements Terminable {

    private static final int PENDING = 0;
    private static final int RUNNING = 1;
    private static final int CANCELLED = 2;

    @ConfigEntry(section = "Storage", comment = "Worker threads for asynchronous operations, operations of one player always run in order. Use 1 for SQLite. Requires a restart")
    private int threads = 4;

    @ConfigEntry(section = "Storage", comment = "Seconds the server waits on shutdown for queued asynchronous operations before failing them")
    private int shutdownTimeoutSeconds = 30;

    @ConfigEntry(section = "Storage", comment = "Milliseconds another thread waits for the main thread to change a PDC, LEVEL or EXP balance")
    private int mainThreadTimeoutMillis = 5000;

    private final Object lock = new Object();
    private volatile @Nullable ThreadPoolExecutor[] workers;
    private volatile boolean closed;

    @Inject
    public TransactionExecutor(InfPoints plugin) {
        Config.global.registerConfigObject(this);
        plugin.bind(this);
    }

    public <T> @NotNull CompletableFuture<T> submit(@NotNull UUID key, @NotNull Object description, @NotNull Supplier<T> task, @NotNull Supplier<T> fallback) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Job<T> job = new Job<>(description, task, fallback, future);
        if (closed) {
            job.abort();
            return future;
        }
        try {
            ThreadPoolExecutor[] current = workers();
            current[Math.floorMod(key.hashCode(), current.length)].execute(job);
        } catch (RejectedExecutionException e) {
            job.abort();
        }
        return future;
    }

    // The task only starts if it hasn't timed out yet, so a caller that gave up knows nothing was applied
    public <T> T callOnMainThread(@NotNull Supplier<T> task, @NotNull Supplier<T> fallback) {
        if (Bukkit.isPrimaryThread())
            return task.get();
        AtomicInteger state = new AtomicInteger(PENDING);
        CompletableFuture<T> future = new CompletableFuture<>();
        try {
            Schedulers.sync().run(() -> {
                if (!state.compareAndSet(PENDING, RUNNING))
                    return;
                try {
                    future.complete(task.get());
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (RuntimeException e) {
            return fallback.get();
        }
        try {
            return future.get(mainThreadTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            if (state.compareAndSet(PENDING, CANCELLED)) {
                InfPoints.logger().warn("The main thread didn't run an InfPoints operation within {} ms, it was cancelled", mainThreadTimeoutMillis);
                return fallback.get();
            }
            return awaitStarted(future, fallback);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (state.compareAndSet(PENDING, CANCELLED))
                return fallback.get();
            return awaitStarted(future, fallback);
        } catch (ExecutionException e) {
            InfPoints.logger().error("InfPoints operation on the main thread failed", e.getCause());
            return fallback.get();
        }
    }

    public <T> @NotNull CompletableFuture<T> supplyOnMainThread(@NotNull Supplier<T> task, @NotNull Supplier<T> fallback) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Runnable runnable = () -> {
            try {
                future.complete(task.get());
            } catch (Throwable t) {
                InfPoints.logger().error("InfPoints operation on the main thread failed", t);
                future.complete(fallback.get());
            }
        };
        if (closed) {
            future.complete(fallback.get());
        } else if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            try {
                Schedulers.sync().run(runnable);
            } catch (RuntimeException e) {
                future.complete(fallback.get());
            }
        }
        return future;
    }

    public void runOnMainThread(@NotNull Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }
        try {
            Schedulers.sync().run(runnable);
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public void close() {
        ThreadPoolExecutor[] current;
        synchronized (lock) {
            if (closed)
                return;
            closed = true;
            current = workers;
        }
        if (current == null)
            return;
        for (ThreadPoolExecutor worker : current)
            worker.shutdown();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(Math.max(0, shutdownTimeoutSeconds));
        try {
            for (ThreadPoolExecutor worker : current)
                worker.awaitTermination(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Queued operations are failed explicitly, running ones are left to finish rather than being interrupted mid-transaction
        List<Runnable> remaining = new ArrayList<>();
        for (ThreadPoolExecutor worker : current)
            worker.getQueue().drainTo(remaining);
        if (!remaining.isEmpty())
            InfPoints.logger().error("InfPoints shut down with {} queued operations that were not applied, each is listed below", remaining.size());
        for (Runnable runnable : remaining) {
            if (runnable instanceof Job<?> job) {
                InfPoints.logger().error("InfPoints shut down before a queued operation ran, it was not applied: {}", job.description);
                job.abort();
            }
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    private ThreadPoolExecutor[] workers() {
        ThreadPoolExecutor[] current = workers;
        if (current != null)
            return current;
        synchronized (lock) {
            if (closed)
                throw new RejectedExecutionException("InfPoints is shutting down");
            if (workers == null) {
                int count = Math.max(1, threads);
                ThreadPoolExecutor[] created = new ThreadPoolExecutor[count];
                for (int i = 0; i < count; i++) {
                    String name = "InfPoints Worker #" + i;
                    created[i] = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), runnable -> {
                        Thread thread = new Thread(runnable, name);
                        thread.setDaemon(true);
                        return thread;
                    });
                }
                workers = created;
            }
            return workers;
        }
    }

    private static <T> T awaitStarted(CompletableFuture<T> future, Supplier<T> fallback) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fallback.get();
        } catch (ExecutionException e) {
            InfPoints.logger().error("InfPoints operation on the main thread failed", e.getCause());
            return fallback.get();
        }
    }

    private record Job<T>(Object description, Supplier<T> task, Supplier<T> fallback,
                          CompletableFuture<T> future) implements Runnable {

        @Override
            public void run() {
                try {
                    future.complete(task.get());
                } catch (Throwable t) {
                    InfPoints.logger().error("Asynchronous InfPoints operation failed: {}", description, t);
                    future.complete(fallback.get());
                }
            }

            private void abort() {
                future.complete(fallback.get());
            }

        }

}
