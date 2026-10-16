package com.aquaticstudios.aquagraves.platform;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public final class FoliaPlatformScheduler implements PlatformScheduler {
    private final Plugin plugin;

    private final Object globalScheduler;
    private final Object regionScheduler;

    private final Method ownedByCurrentRegion;
    private final Method regionExecute;
    private final Method regionRunDelayed;
    private final Method globalExecute;
    private final Method globalRunDelayed;
    private final Method globalRunAtFixedRate;
    private final Method globalCancelTasks;
    private final Method scheduledCancel;

    public FoliaPlatformScheduler(@NotNull Plugin plugin) throws ReflectiveOperationException {
        this.plugin = plugin;

        final Object server = Bukkit.getServer();
        this.globalScheduler = server.getClass().getMethod("getGlobalRegionScheduler").invoke(server);
        this.regionScheduler = server.getClass().getMethod("getRegionScheduler").invoke(server);

        this.ownedByCurrentRegion = server.getClass().getMethod("isOwnedByCurrentRegion", Location.class);
        this.regionExecute = regionScheduler.getClass().getMethod("execute", Plugin.class, Location.class, Runnable.class);
        this.regionRunDelayed = regionScheduler.getClass().getMethod("runDelayed", Plugin.class, Location.class, Consumer.class, long.class);
        this.globalExecute = globalScheduler.getClass().getMethod("execute", Plugin.class, Runnable.class);
        this.globalRunDelayed = globalScheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
        this.globalRunAtFixedRate = globalScheduler.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
        this.globalCancelTasks = globalScheduler.getClass().getMethod("cancelTasks", Plugin.class);
        this.scheduledCancel = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask").getMethod("cancel");
    }

    @Override
    public boolean isRegionised() {
        return true;
    }

    @Override
    public boolean ownsRegion(@NotNull Location location) {
        try {
            return (Boolean) ownedByCurrentRegion.invoke(Bukkit.getServer(), location);
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    @Override
    public void execute(@NotNull Location location, @NotNull Runnable job) {
        if (ownsRegion(location)) {
            job.run();
            return;
        }
        invoke(regionExecute, regionScheduler, plugin, location, job);
    }

    @Override
    public void executeLater(@NotNull Location location, @NotNull Runnable job, long delayTicks) {
        invoke(regionRunDelayed, regionScheduler, plugin, location,
                (Consumer<Object>) ignored -> job.run(), Math.max(1L, delayTicks));
    }

    @Override
    public void global(@NotNull Runnable job) {
        invoke(globalExecute, globalScheduler, plugin, job);
    }

    @Override
    public void globalLater(@NotNull Runnable job, long delayTicks) {
        invoke(globalRunDelayed, globalScheduler, plugin,
                (Consumer<Object>) ignored -> job.run(), Math.max(1L, delayTicks));
    }

    @NotNull
    @Override
    public Task repeating(@NotNull Runnable job, long delayTicks, long periodTicks) {
        final Object handle = invoke(globalRunAtFixedRate, globalScheduler, plugin,
                (Consumer<Object>) ignored -> job.run(), Math.max(1L, delayTicks), Math.max(1L, periodTicks));

        if (handle == null) return () -> {
        };

        return () -> invoke(scheduledCancel, handle);
    }

    @Override
    public void shutdown() {
        invoke(globalCancelTasks, globalScheduler, plugin);
    }

    private Object invoke(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Folia scheduler bridge failed on " + method.getName(), exception);
        }
    }
}
