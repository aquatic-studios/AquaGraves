package com.aquaticstudios.aquagraves.io;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class IoWorker {
    private final ExecutorService executor;
    private final Logger logger;

    public IoWorker(@NotNull String name, @NotNull Logger logger) {
        this.logger = logger;

        final ThreadFactory factory = runnable -> {
            final Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        };
        this.executor = Executors.newSingleThreadExecutor(factory);
    }

    @NotNull
    public CompletableFuture<Void> run(@NotNull Runnable job) {
        if (executor.isShutdown()) return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(job, executor).exceptionally(throwable -> {
            logger.log(Level.SEVERE, "Background IO task failed", throwable);
            return null;
        });
    }

    @NotNull
    public <T> CompletableFuture<T> supply(@NotNull Supplier<T> job) {
        if (executor.isShutdown()) return CompletableFuture.completedFuture(null);

        return CompletableFuture.supplyAsync(job, executor).exceptionally(throwable -> {
            logger.log(Level.SEVERE, "Background IO task failed", throwable);
            return null;
        });
    }

    public boolean isRunning() {
        return !executor.isShutdown();
    }

    public void shutdown(long timeoutMillis) {
        executor.shutdown();
        try {
            if (executor.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS)) return;

            logger.warning("IO worker did not finish within " + timeoutMillis + "ms, forcing it down");
            executor.shutdownNow();
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
