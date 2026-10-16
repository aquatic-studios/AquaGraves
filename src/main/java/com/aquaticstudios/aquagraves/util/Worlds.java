package com.aquaticstudios.aquagraves.util;

import com.aquaticstudios.aquagraves.config.GraveConfig;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public final class Worlds {
    @NotNull
    public static Location center(@NotNull Location location) {
        final Location centered = new Location(
                location.getWorld(),
                location.getBlockX() + 0.5D,
                location.getBlockY() + 0.5D,
                location.getBlockZ() + 0.5D,
                location.getYaw(),
                0.0f);
        return centered;
    }

    public static float nearestDirection(float yaw) {
        return Math.round(yaw / 90.0f) * 90.0f;
    }

    public static void clampHeight(@NotNull Location location, @NotNull GraveConfig config) {
        final World world = location.getWorld();
        if (world == null) return;

        final double[] configured = config.heightLimit(world.getName());

        final double min;
        final double max;
        if (configured != null) {
            min = configured[0];
            max = configured[1];
        } else {
            switch (world.getEnvironment()) {
                case NETHER:
                case THE_END:
                    min = 0.0D;
                    max = 255.0D;
                    break;
                default:
                    min = world.getMinHeight();
                    max = world.getMaxHeight() - 1;
                    break;
            }
        }

        if (location.getY() < min) location.setY(min);
        else if (location.getY() > max) location.setY(max);
    }
}
