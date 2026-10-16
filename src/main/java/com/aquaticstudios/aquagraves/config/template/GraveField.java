package com.aquaticstudios.aquagraves.config.template;

import org.jetbrains.annotations.NotNull;

public final class GraveField {
    public static final int PLAYER = 0;
    public static final int ITEMS = 1;
    public static final int STACKS = 2;
    public static final int XP = 3;
    public static final int DESPAWN_TIME = 4;
    public static final int PROTECTION_TIME = 5;
    public static final int LOCK_STATUS = 6;
    public static final int AGE = 7;
    public static final int VIEWERS = 8;
    public static final int WORLD = 9;
    public static final int X = 10;
    public static final int Y = 11;
    public static final int Z = 12;
    public static final int ID = 13;

    public static final int COUNT = 14;

    private static final String[] TOKENS = {
            "%player%", "%items%", "%stacks%", "%xp%", "%despawn-time%", "%protection-time%",
            "%lock-status%", "%age%", "%viewers%", "%world%", "%x%", "%y%", "%z%", "%id%"
    };

    @NotNull
    public static String token(int field) {
        return TOKENS[field];
    }

    @NotNull
    public static String[] tokens() {
        return TOKENS.clone();
    }

    public interface Source {
        void appendField(int field, @NotNull StringBuilder out);
    }
}
