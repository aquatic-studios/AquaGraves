package com.aquaticstudios.aquagraves.platform;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public interface PlatformScheduler {
    interface Task {
        void cancel();
    }

    boolean isRegionised();

    boolean ownsRegion(@NotNull Location location);

    void execute(@NotNull Location location, @NotNull Runnable job);

    void executeLater(@NotNull Location location, @NotNull Runnable job, long delayTicks);

    void global(@NotNull Runnable job);

    void globalLater(@NotNull Runnable job, long delayTicks);

    @NotNull
    Task repeating(@NotNull Runnable job, long delayTicks, long periodTicks);

    void shutdown();
}
