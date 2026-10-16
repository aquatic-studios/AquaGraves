package com.aquaticstudios.aquagraves.grave;

import com.aquaticstudios.aquagraves.collection.LongHashSet;
import com.aquaticstudios.aquagraves.config.GraveConfig;
import com.aquaticstudios.aquagraves.config.MessageBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GraveTick {
    public final long now;
    public final GraveConfig config;
    public final MessageBundle messages;

    private final LongHashSet occupiedChunks;
    private final int viewChunks;
    private final boolean forceVisible;

    private GraveTick(long now, GraveConfig config, MessageBundle messages,
                      LongHashSet occupiedChunks, boolean forceVisible) {
        this.now = now;
        this.config = config;
        this.messages = messages;
        this.occupiedChunks = occupiedChunks;
        this.viewChunks = config.hologramViewChunks;
        this.forceVisible = forceVisible;
    }

    @NotNull
    public static GraveTick of(long now, @NotNull GraveConfig config, @NotNull MessageBundle messages,
                               @Nullable LongHashSet occupiedChunks) {
        return new GraveTick(now, config, messages, occupiedChunks, false);
    }

    @NotNull
    public static GraveTick immediate(@NotNull GraveConfig config, @NotNull MessageBundle messages) {
        return new GraveTick(System.currentTimeMillis(), config, messages, null, true);
    }

    public boolean isVisible(long worldSeed, int chunkX, int chunkZ) {
        if (forceVisible) return true;
        if (occupiedChunks == null || occupiedChunks.isEmpty()) return false;

        for (int dx = -viewChunks; dx <= viewChunks; dx++) {
            for (int dz = -viewChunks; dz <= viewChunks; dz++) {
                if (occupiedChunks.contains(GraveKeys.chunk(worldSeed, chunkX + dx, chunkZ + dz))) return true;
            }
        }
        return false;
    }
}
