package com.aquaticstudios.aquagraves.grave;

public final class GraveKeys {
    public static long block(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38)
                | ((long) (z & 0x3FFFFFF) << 12)
                | (y & 0xFFF);
    }

    public static long chunk(long worldSeed, int chunkX, int chunkZ) {
        final long packed = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
        return packed * 0x9E3779B97F4A7C15L + worldSeed;
    }
}
