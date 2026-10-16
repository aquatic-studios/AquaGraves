package com.aquaticstudios.aquagraves.grave;

import com.aquaticstudios.aquagraves.collection.LongHashSet;
import com.aquaticstudios.aquagraves.config.ConfigService;
import com.aquaticstudios.aquagraves.config.GraveConfig;
import com.aquaticstudios.aquagraves.io.GraveRepository;
import com.aquaticstudios.aquagraves.platform.PlatformScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class GraveTicker {
    private final ConfigService configs;
    private final PlatformScheduler scheduler;
    private final GraveRegistry registry;
    private final GraveRepository repository;
    private final Logger logger;

    private final LongHashSet occupiedChunks = new LongHashSet(512);

    private PlatformScheduler.Task task;
    private long intervalTicks;
    private long tickCounter;

    public GraveTicker(@NotNull ConfigService configs,
                       @NotNull PlatformScheduler scheduler,
                       @NotNull GraveRegistry registry,
                       @NotNull GraveRepository repository,
                       @NotNull Logger logger) {
        this.configs = configs;
        this.scheduler = scheduler;
        this.registry = registry;
        this.repository = repository;
        this.logger = logger;
    }

    public synchronized void ensureRunning() {
        if (task != null) return;
        if (registry.isEmpty()) return;

        intervalTicks = configs.config().hologramIntervalTicks;
        task = scheduler.repeating(this::run, intervalTicks, intervalTicks);
    }

    public synchronized void restart() {
        stop();
        ensureRunning();
    }

    public synchronized void stop() {
        if (task == null) return;
        task.cancel();
        task = null;
    }

    public synchronized boolean isRunning() {
        return task != null;
    }

    private void run() {
        if (registry.isEmpty()) {
            stop();
            return;
        }

        final GraveConfig config = configs.config();
        final boolean holograms = config.hologramEnabled;

        if (holograms) collectOccupiedChunks();

        final GraveTick pass = GraveTick.of(System.currentTimeMillis(), config, configs.messages(),
                holograms ? occupiedChunks : null);

        final boolean regionised = scheduler.isRegionised();

        for (Grave grave : registry.all()) {
            if (grave.isRemoved()) continue;

            try {
                if (regionised) {
                    final Grave target = grave;
                    scheduler.execute(target.locationRef(), () -> target.tick(pass));
                } else {
                    grave.tick(pass);
                }
            } catch (Exception exception) {
                logger.log(Level.WARNING, "Grave " + grave.id() + " failed to tick", exception);
            }
        }

        maybeFlush(config);
    }

    private void collectOccupiedChunks() {
        occupiedChunks.clear();

        for (Player player : Bukkit.getOnlinePlayers()) {
            final Location location = player.getLocation();
            final World world = location.getWorld();
            if (world == null) continue;

            occupiedChunks.add(GraveKeys.chunk(
                    world.getUID().getLeastSignificantBits(),
                    location.getBlockX() >> 4,
                    location.getBlockZ() >> 4));
        }
    }

    private void maybeFlush(GraveConfig config) {
        if (config.saveOnChange) return;
        if (config.saveIntervalTicks <= 0L) return;

        tickCounter += intervalTicks;
        if (tickCounter < config.saveIntervalTicks) return;

        tickCounter = 0L;
        repository.flushIfDirty();
    }
}
