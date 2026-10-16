package com.aquaticstudios.aquagraves.config;

import com.aquaticstudios.aquagraves.grave.LimitAction;
import com.aquaticstudios.aquagraves.grave.ProtectionAction;
import com.aquaticstudios.aquagraves.hologram.HologramType;
import com.aquaticstudios.aquagraves.util.CC;
import dev.dejvokep.boostedyaml.YamlDocument;
import org.bukkit.Material;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public final class GraveConfig {
    public final int despawnSeconds;
    public final long despawnMillis;
    public final boolean despawnWhenEmpty;
    public final boolean dropItemsOnDespawn;
    public final boolean droppedItemVelocity;
    public final int graveLimit;
    public final LimitAction limitReached;
    public final double maxInteractDistanceSquared;
    public final boolean allowDeposits;
    public final boolean closeGuiWhenFar;

    public final boolean protectionEnabled;
    public final int protectionSeconds;
    public final ProtectionAction protectionExpire;
    public final boolean protectionNotify;
    public final boolean alwaysOwnerOnly;

    public final boolean storeItems;
    public final boolean storeXp;
    public final float xpKeepPercentage;
    public final boolean overrideKeepInventory;
    public final boolean overrideKeepLevel;
    public final EventPriority deathPriority;
    public final Set<String> disabledWorlds;
    public final Set<String> deniedDeathCauses;
    public final List<String> itemOrder;
    public final boolean sendCoordinates;

    public final boolean instantPickup;
    public final boolean instantPickupOnlyOwn;
    public final boolean autoEquipArmor;
    public final boolean xpOnInteract;

    public final double headHeight;
    public final boolean rotateHead360;
    public final boolean autoRotation;
    public final float autoRotationSpeed;
    public final boolean customSkull;
    public final String customSkullBase64;

    public final boolean hologramEnabled;
    public final HologramType hologramType;
    public final double hologramHeight;
    public final double hologramLineHeight;
    public final double hologramViewDistance;

    public final int hologramViewChunks;
    public final long hologramIntervalTicks;
    public final int hologramBackground;
    public final String hologramAlignment;
    public final String hologramBillboard;
    public final boolean hologramSeeThrough;
    public final boolean hologramShadow;

    public final boolean teleportEnabled;
    public final boolean teleportOnlyOwn;
    public final long teleportCooldownMillis;
    public final int teleportWarmupSeconds;
    public final long teleportWarmupTicks;
    public final boolean teleportCancelOnMove;
    public final boolean teleportCancelOnDamage;

    public final int saveIntervalSeconds;
    public final long saveIntervalTicks;
    public final boolean saveOnChange;

    public final long saveCoalesceTicks;
    public final boolean backupsEnabled;
    public final int backupsPerPlayer;

    private final Map<String, double[]> heightLimits;
    private final Map<String, String> worldNames;

    public final Set<Material> deniedMaterials;
    public final List<String> deniedNames;

    public final String soundSpawn;
    public final String soundOpen;
    public final String soundPickup;
    public final String soundUnlock;
    public final String soundDespawn;
    public final float soundVolume;
    public final float soundPitch;

    public final List<String> commandAliases;
    public final boolean updateNotify;
    public final boolean debug;

    private GraveConfig(@NotNull YamlDocument yaml, @NotNull Logger logger) {
        despawnSeconds = yaml.getInt("grave.despawn-time-seconds", 300);
        despawnMillis = despawnSeconds < 0 ? Long.MAX_VALUE : despawnSeconds * 1000L;
        despawnWhenEmpty = yaml.getBoolean("grave.despawn-when-empty", true);
        dropItemsOnDespawn = yaml.getBoolean("grave.drop-items-on-despawn", true);
        droppedItemVelocity = yaml.getBoolean("grave.dropped-item-velocity", true);
        graveLimit = yaml.getInt("grave.limit", 3);
        limitReached = parseEnum(LimitAction.class, yaml.getString("grave.limit-reached"), LimitAction.REMOVE_OLDEST);
        final double interact = Math.max(1.0D, yaml.getDouble("grave.max-interact-distance", 6.0D));
        maxInteractDistanceSquared = interact * interact;
        allowDeposits = yaml.getBoolean("grave.allow-deposits", true);
        closeGuiWhenFar = yaml.getBoolean("grave.close-gui-when-far", true);

        protectionEnabled = yaml.getBoolean("protection.enabled", true);
        protectionSeconds = Math.max(0, yaml.getInt("protection.duration-seconds", 120));
        protectionExpire = parseEnum(ProtectionAction.class, yaml.getString("protection.on-expire"), ProtectionAction.UNLOCK);
        protectionNotify = yaml.getBoolean("protection.notify-owner", true);
        alwaysOwnerOnly = yaml.getBoolean("protection.always-owner-only", false);

        storeItems = yaml.getBoolean("death.store-items", true);
        storeXp = yaml.getBoolean("death.store-xp", true);
        xpKeepPercentage = clamp01(yaml.getFloat("death.xp-keep-percentage", 1.0f));
        overrideKeepInventory = yaml.getBoolean("death.override-keep-inventory", false);
        overrideKeepLevel = yaml.getBoolean("death.override-keep-level", false);
        deathPriority = parseEnum(EventPriority.class, yaml.getString("death.listener-priority"), EventPriority.HIGHEST);
        disabledWorlds = immutableSet(yaml.getStringList("death.disabled-worlds"), false);
        deniedDeathCauses = immutableSet(yaml.getStringList("death.blacklisted-death-causes"), true);
        itemOrder = immutableUpperList(yaml.getStringList("death.item-order"));
        sendCoordinates = yaml.getBoolean("death.send-coordinates", true);

        instantPickup = yaml.getBoolean("pickup.instant-pickup", true);
        instantPickupOnlyOwn = yaml.getBoolean("pickup.instant-pickup-only-own", false);
        autoEquipArmor = yaml.getBoolean("pickup.auto-equip-armor", true);
        xpOnInteract = yaml.getBoolean("pickup.xp-on-interact", true);

        headHeight = yaml.getDouble("appearance.head-height", -1.2D);
        rotateHead360 = yaml.getBoolean("appearance.rotate-head-360", true);
        autoRotation = yaml.getBoolean("appearance.auto-rotation.enabled", false);
        autoRotationSpeed = yaml.getFloat("appearance.auto-rotation.speed", 10.0f);
        customSkull = yaml.getBoolean("appearance.custom-skull.enabled", false);
        customSkullBase64 = yaml.getString("appearance.custom-skull.base64", "").trim();

        hologramEnabled = yaml.getBoolean("appearance.hologram.enabled", true);
        hologramType = parseEnum(HologramType.class, yaml.getString("appearance.hologram.type"), HologramType.AUTO);
        hologramHeight = yaml.getDouble("appearance.hologram.height", 0.75D);
        hologramLineHeight = yaml.getDouble("appearance.hologram.line-height", 0.28D);
        hologramViewDistance = Math.max(8.0D, yaml.getDouble("appearance.hologram.view-distance", 48.0D));
        hologramViewChunks = Math.max(1, (int) Math.ceil(hologramViewDistance / 16.0D));
        hologramIntervalTicks = Math.max(1L, yaml.getLong("appearance.hologram.update-interval-ticks", 20L));
        hologramBackground = parseColor(yaml.getString("appearance.hologram.background-color", "00000000"));
        hologramAlignment = upper(yaml.getString("appearance.hologram.alignment", "CENTER"));
        hologramBillboard = upper(yaml.getString("appearance.hologram.billboard", "VERTICAL"));
        hologramSeeThrough = yaml.getBoolean("appearance.hologram.see-through", false);
        hologramShadow = yaml.getBoolean("appearance.hologram.shadow", true);

        teleportEnabled = yaml.getBoolean("teleport.enabled", true);
        teleportOnlyOwn = yaml.getBoolean("teleport.only-own", true);
        teleportCooldownMillis = Math.max(0, yaml.getInt("teleport.cooldown-seconds", 30)) * 1000L;
        teleportWarmupSeconds = Math.max(0, yaml.getInt("teleport.warmup-seconds", 3));
        teleportWarmupTicks = teleportWarmupSeconds * 20L;
        teleportCancelOnMove = yaml.getBoolean("teleport.cancel-on-move", true);
        teleportCancelOnDamage = yaml.getBoolean("teleport.cancel-on-damage", true);

        saveIntervalSeconds = yaml.getInt("storage.save-interval-seconds", 60);
        saveIntervalTicks = saveIntervalSeconds <= 0 ? -1L : saveIntervalSeconds * 20L;
        saveOnChange = yaml.getBoolean("storage.save-on-change", true);
        saveCoalesceTicks = Math.max(20L, yaml.getLong("storage.save-coalesce-ticks", 100L));
        backupsEnabled = yaml.getBoolean("storage.backups.enabled", true);
        backupsPerPlayer = Math.max(1, yaml.getInt("storage.backups.keep-per-player", 5));

        heightLimits = readHeightLimits(yaml);
        worldNames = readWorldNames(yaml);

        deniedMaterials = readMaterials(yaml, logger);
        deniedNames = readDeniedNames(yaml);

        soundSpawn = trimmed(yaml.getString("sounds.spawn", ""));
        soundOpen = trimmed(yaml.getString("sounds.open", ""));
        soundPickup = trimmed(yaml.getString("sounds.pickup", ""));
        soundUnlock = trimmed(yaml.getString("sounds.unlock", ""));
        soundDespawn = trimmed(yaml.getString("sounds.despawn", ""));
        soundVolume = yaml.getFloat("sounds.volume", 0.6f);
        soundPitch = yaml.getFloat("sounds.pitch", 1.4f);

        commandAliases = readAliases(yaml);
        updateNotify = yaml.getBoolean("update-notify", true);
        debug = yaml.getBoolean("debug", false);
    }

    @NotNull
    public static GraveConfig from(@NotNull YamlDocument yaml, @NotNull Logger logger) {
        return new GraveConfig(yaml, logger);
    }

    @Nullable
    public double[] heightLimit(@NotNull String world) {
        return heightLimits.get(world);
    }

    @NotNull
    public String worldName(@NotNull String world) {
        final String display = worldNames.get(world);
        return display == null ? world : display;
    }

    private static Map<String, double[]> readHeightLimits(YamlDocument yaml) {
        if (yaml.getSection("spawn-height-limits") == null) return Collections.emptyMap();

        final Map<String, double[]> limits = new HashMap<>();
        for (Object key : yaml.getSection("spawn-height-limits").getRoutesAsStrings(false)) {
            final String world = String.valueOf(key);
            final String path = "spawn-height-limits." + world;
            limits.put(world, new double[]{
                    yaml.getDouble(path + ".min", -64.0D),
                    yaml.getDouble(path + ".max", 319.0D)});
        }
        return Collections.unmodifiableMap(limits);
    }

    private static Map<String, String> readWorldNames(YamlDocument yaml) {
        if (yaml.getSection("world-name") == null) return Collections.emptyMap();

        final Map<String, String> names = new HashMap<>();
        for (Object key : yaml.getSection("world-name").getRoutesAsStrings(false)) {
            final String world = String.valueOf(key);
            names.put(world, yaml.getString("world-name." + world, world));
        }
        return Collections.unmodifiableMap(names);
    }

    private static Set<Material> readMaterials(YamlDocument yaml, Logger logger) {
        final Set<Material> materials = EnumSet.noneOf(Material.class);
        for (String entry : yaml.getStringList("denied-section.items")) {
            if (entry == null || entry.trim().isEmpty()) continue;

            final Material material = Material.matchMaterial(entry.trim().toUpperCase(Locale.ROOT));
            if (material == null) {
                logger.warning("Unknown material in denied-section.items: " + entry);
                continue;
            }
            materials.add(material);
        }
        return materials.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(materials);
    }

    private static List<String> readDeniedNames(YamlDocument yaml) {
        final List<String> names = new ArrayList<>();
        for (String entry : yaml.getStringList("denied-section.names")) {
            if (entry == null || entry.trim().isEmpty()) continue;
            names.add(CC.strip(entry).toLowerCase(Locale.ROOT));
        }
        return names.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(names);
    }

    private static List<String> readAliases(YamlDocument yaml) {
        final List<String> aliases = new ArrayList<>();
        for (String alias : yaml.getStringList("command-aliases")) {
            if (alias == null || alias.trim().isEmpty()) continue;

            final String label = alias.trim().toLowerCase(Locale.ROOT);
            if (!aliases.contains(label)) aliases.add(label);
        }
        if (aliases.isEmpty()) {
            aliases.add("aquagraves");
            aliases.add("agraves");
            aliases.add("graves");
            aliases.add("grave");
        }
        return Collections.unmodifiableList(aliases);
    }

    private static Set<String> immutableSet(List<String> input, boolean upperCase) {
        final Set<String> out = new HashSet<>();
        for (String entry : input) {
            if (entry == null || entry.trim().isEmpty()) continue;
            out.add(upperCase ? entry.trim().toUpperCase(Locale.ROOT) : entry.trim());
        }
        return out.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(out);
    }

    private static List<String> immutableUpperList(List<String> input) {
        final List<String> out = new ArrayList<>(input.size());
        for (String entry : input) {
            if (entry == null || entry.trim().isEmpty()) continue;
            out.add(entry.trim().toUpperCase(Locale.ROOT));
        }
        return out.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(out);
    }

    private static String trimmed(String value) {
        return value == null ? "" : value.trim();
    }

    private static String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static float clamp01(float value) {
        if (value < 0.0f) return 0.0f;
        return Math.min(value, 1.0f);
    }

    private static int parseColor(String raw) {
        try {
            return (int) Long.parseLong(raw.trim().replace("#", ""), 16);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> type, String name, T fallback) {
        if (name == null) return fallback;
        try {
            return Enum.valueOf(type, name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
