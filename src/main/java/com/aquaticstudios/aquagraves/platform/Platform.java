package com.aquaticstudios.aquagraves.platform;

public enum Platform {
    SPIGOT(false),
    PAPER(false),
    PURPUR(false),
    PUFFERFISH(false),
    FOLIA(true);

    private static final Platform CURRENT = detect();

    private final boolean regionised;

    Platform(boolean regionised) {
        this.regionised = regionised;
    }

    public static Platform current() {
        return CURRENT;
    }

    public boolean isRegionised() {
        return regionised;
    }

    public static boolean isFolia() {
        return CURRENT.regionised;
    }

    public static boolean classPresent(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    private static Platform detect() {
        if (classPresent("io.papermc.paper.threadedregions.RegionizedServer")) return FOLIA;
        if (classPresent("org.purpurmc.purpur.PurpurConfig")) return PURPUR;
        if (classPresent("gg.pufferfish.pufferfish.PufferfishConfig")) return PUFFERFISH;
        if (classPresent("com.destroystokyo.paper.PaperConfig")
                || classPresent("io.papermc.paper.configuration.Configuration")) return PAPER;
        return SPIGOT;
    }
}
