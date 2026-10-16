package com.aquaticstudios.aquagraves.util;

import com.aquaticstudios.aquagraves.config.GraveConfig;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Sounds {
    public static void play(@Nullable Player player, @Nullable String sound, @NotNull GraveConfig config) {
        if (player == null || sound == null || sound.isEmpty()) return;

        try {
            player.playSound(player.getLocation(), sound, config.soundVolume, config.soundPitch);
        } catch (RuntimeException ignored) {
        }
    }

    public static void playAt(@NotNull Location location, @Nullable String sound, @NotNull GraveConfig config) {
        if (sound == null || sound.isEmpty()) return;

        final World world = location.getWorld();
        if (world == null) return;

        try {
            world.playSound(location, sound, config.soundVolume, config.soundPitch);
        } catch (RuntimeException ignored) {
        }
    }
}
