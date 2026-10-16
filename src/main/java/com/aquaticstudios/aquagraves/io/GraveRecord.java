package com.aquaticstudios.aquagraves.io;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class GraveRecord {
    public final UUID id;
    public final UUID owner;
    public final String ownerName;
    public final String world;
    public final double x;
    public final double y;
    public final double z;
    public final float yaw;
    public final float pitch;
    public final long created;
    public final long protectedUntil;
    public final int xp;
    public final String contents;

    public final String line;

    GraveRecord(UUID id, UUID owner, String ownerName, String world,
                double x, double y, double z, float yaw, float pitch,
                long created, long protectedUntil, int xp, String contents, String line) {
        this.id = id;
        this.owner = owner;
        this.ownerName = ownerName;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.created = created;
        this.protectedUntil = protectedUntil;
        this.xp = xp;
        this.contents = contents;
        this.line = line;
    }

    @NotNull
    @Override
    public String toString() {
        return "GraveRecord(" + id + " of " + ownerName + " in " + world + ")";
    }
}
