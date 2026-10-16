package com.aquaticstudios.aquagraves.platform;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public final class SchedulerFactory {
    @NotNull
    public static PlatformScheduler create(@NotNull Plugin plugin) {
        if (!Platform.isFolia()) return new BukkitPlatformScheduler(plugin);

        try {
            return new FoliaPlatformScheduler(plugin);
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().log(Level.WARNING,
                    "Folia was detected but its scheduler could not be bound, falling back to the Bukkit scheduler",
                    exception);
            return new BukkitPlatformScheduler(plugin);
        }
    }
}
