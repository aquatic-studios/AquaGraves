package com.aquaticstudios.aquagraves.grave;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public final class GraveEntities {
    public static final String TAG = "aquagraves";
    private static final String ID_PREFIX = "aquagraves_id_";

    public static boolean couldBeGraveEntity(@NotNull EntityType type) {
        return type == EntityType.ARMOR_STAND || "TEXT_DISPLAY".equals(type.name());
    }

    public static void tag(@NotNull Entity entity, @NotNull UUID graveId) {
        entity.addScoreboardTag(TAG);
        entity.addScoreboardTag(ID_PREFIX + graveId);
    }

    public static boolean isGraveEntity(@NotNull Entity entity) {
        if (!couldBeGraveEntity(entity.getType())) return false;
        return entity.getScoreboardTags().contains(TAG);
    }

    @Nullable
    public static UUID graveIdOf(@NotNull Entity entity) {
        final Set<String> tags = entity.getScoreboardTags();
        for (String tag : tags) {
            if (!tag.startsWith(ID_PREFIX)) continue;
            try {
                return UUID.fromString(tag.substring(ID_PREFIX.length()));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    public static int cleanup(@NotNull Chunk chunk, @NotNull GraveRegistry registry) {
        int removed = 0;

        for (Entity entity : chunk.getEntities()) {
            if (!couldBeGraveEntity(entity.getType())) continue;
            if (!entity.getScoreboardTags().contains(TAG)) continue;

            final UUID graveId = graveIdOf(entity);
            if (graveId != null && registry.byId(graveId) != null) continue;

            entity.remove();
            removed++;
        }

        return removed;
    }

    public static int cleanupLoaded(@NotNull GraveRegistry registry) {
        int removed = 0;

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                removed += cleanup(chunk, registry);
            }
        }

        return removed;
    }
}
