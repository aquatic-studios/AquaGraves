package com.aquaticstudios.aquagraves.grave;

public enum RemoveReason {
    EXPIRED(true, true),

    PROTECTION_EXPIRED(true, true),

    EMPTIED(false, false),

    LIMIT(true, true),

    COMMAND(true, true),

    PURGE(false, true),

    PLUGIN(true, true);

    private final boolean drops;
    private final boolean backup;

    RemoveReason(boolean drops, boolean backup) {
        this.drops = drops;
        this.backup = backup;
    }

    public boolean drops() {
        return drops;
    }

    public boolean backup() {
        return backup;
    }
}
