package com.aquaticstudios.aquagraves.platform;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public final class BukkitPlatformScheduler implements PlatformScheduler {
    private final Plugin plugin;

    public BukkitPlatformScheduler(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isRegionised() {
        return false;
    }

    @Override
    public boolean ownsRegion(@NotNull Location location) {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public void execute(@NotNull Location location, @NotNull Runnable job) {
        if (Bukkit.isPrimaryThread()) {
            job.run();
            return;
        }
        Bukkit.getScheduler().runTask(plugin, job);
    }

    @Override
    public void executeLater(@NotNull Location location, @NotNull Runnable job, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, job, Math.max(1L, delayTicks));
    }

    @Override
    public void global(@NotNull Runnable job) {
        if (Bukkit.isPrimaryThread()) {
            job.run();
            return;
        }
        Bukkit.getScheduler().runTask(plugin, job);
    }

    @Override
    public void globalLater(@NotNull Runnable job, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, job, Math.max(1L, delayTicks));
    }

    @NotNull
    @Override
    public Task repeating(@NotNull Runnable job, long delayTicks, long periodTicks) {
        final BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, job,
                Math.max(1L, delayTicks), Math.max(1L, periodTicks));
        return task::cancel;
    }

    @Override
    public void shutdown() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }
}
