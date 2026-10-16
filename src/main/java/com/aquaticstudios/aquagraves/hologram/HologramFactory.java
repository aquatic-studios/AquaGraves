package com.aquaticstudios.aquagraves.hologram;

import com.aquaticstudios.aquagraves.config.GraveConfig;
import com.aquaticstudios.aquagraves.hologram.impl.ArmorStandHologram;
import com.aquaticstudios.aquagraves.hologram.impl.TextDisplayHologram;
import com.aquaticstudios.aquagraves.platform.Platform;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class HologramFactory {
    private static final boolean TEXT_DISPLAYS_AVAILABLE = detectTextDisplays();

    @NotNull
    public static Hologram create(@NotNull UUID graveId, @NotNull GraveConfig config) {
        return usesTextDisplays(config)
                ? new TextDisplayHologram(graveId, config)
                : new ArmorStandHologram(graveId, config);
    }

    @NotNull
    public static String describe(@NotNull GraveConfig config) {
        return usesTextDisplays(config) ? "TEXT_DISPLAY" : "ARMOR_STAND";
    }

    private static boolean usesTextDisplays(GraveConfig config) {
        if (config.hologramType == HologramType.ARMOR_STAND) return false;
        return TEXT_DISPLAYS_AVAILABLE;
    }

    private static boolean detectTextDisplays() {
        if (!Platform.classPresent("org.bukkit.entity.TextDisplay")) return false;

        try {
            EntityType.valueOf("TEXT_DISPLAY");
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
