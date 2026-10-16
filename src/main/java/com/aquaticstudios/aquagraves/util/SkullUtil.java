package com.aquaticstudios.aquagraves.util;

import com.aquaticstudios.aquagraves.config.GraveConfig;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SkullUtil {
    private static final Pattern SKIN_URL = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");
    private static final UUID TEXTURE_OWNER = UUID.nameUUIDFromBytes("AquaGraves".getBytes(StandardCharsets.UTF_8));

    private static final Material HEAD = resolveHead();

    private static Method createProfile;
    private static Method setOwnerProfile;
    private static boolean profileApiChecked;

    private static volatile String cachedTexture;
    private static volatile ItemStack cachedCustomSkull;

    @NotNull
    public static ItemStack graveSkull(@Nullable OfflinePlayer owner, @NotNull GraveConfig config) {
        if (config.customSkull && !config.customSkullBase64.isEmpty()) {
            return customSkull(config.customSkullBase64).clone();
        }

        final ItemStack item = new ItemStack(HEAD);
        if (owner == null) return item;

        final org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof SkullMeta)) return item;

        ((SkullMeta) meta).setOwningPlayer(owner);
        item.setItemMeta(meta);
        return item;
    }

    public static void invalidate() {
        cachedTexture = null;
        cachedCustomSkull = null;
    }

    private static ItemStack customSkull(String base64) {
        final ItemStack cached = cachedCustomSkull;
        if (cached != null && base64.equals(cachedTexture)) return cached;

        final ItemStack item = new ItemStack(HEAD);
        final org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

        if (meta instanceof SkullMeta) {
            applyTexture((SkullMeta) meta, base64);
            item.setItemMeta(meta);
        }

        cachedTexture = base64;
        cachedCustomSkull = item;
        return item;
    }

    private static void applyTexture(@NotNull SkullMeta meta, @NotNull String base64) {
        if (applyWithProfileApi(meta, base64)) return;
        applyWithReflection(meta, base64);
    }

    private static boolean applyWithProfileApi(@NotNull SkullMeta meta, @NotNull String base64) {
        if (!profileApiChecked) {
            profileApiChecked = true;
            try {
                final Class<?> profileType = Class.forName("org.bukkit.profile.PlayerProfile");
                createProfile = Bukkit.class.getMethod("createPlayerProfile", UUID.class, String.class);
                setOwnerProfile = SkullMeta.class.getMethod("setOwnerProfile", profileType);
            } catch (ReflectiveOperationException ignored) {
                createProfile = null;
                setOwnerProfile = null;
            }
        }

        if (createProfile == null || setOwnerProfile == null) return false;

        final String url = decodeSkinUrl(base64);
        if (url == null) return false;

        try {
            final Class<?> texturesType = Class.forName("org.bukkit.profile.PlayerTextures");

            final Object profile = createProfile.invoke(null, TEXTURE_OWNER, "AquaGraves");
            final Object textures = profile.getClass().getMethod("getTextures").invoke(profile);
            texturesType.getMethod("setSkin", URL.class).invoke(textures, new URL(url));
            profile.getClass().getMethod("setTextures", texturesType).invoke(profile, textures);

            setOwnerProfile.invoke(meta, profile);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private static void applyWithReflection(@NotNull SkullMeta meta, @NotNull String base64) {
        try {
            final Class<?> gameProfile = Class.forName("com.mojang.authlib.GameProfile");
            final Class<?> property = Class.forName("com.mojang.authlib.properties.Property");

            final Object profile = gameProfile.getConstructor(UUID.class, String.class)
                    .newInstance(TEXTURE_OWNER, "AquaGraves");
            final Object textures = property.getConstructor(String.class, String.class)
                    .newInstance("textures", base64);

            final Object properties = gameProfile.getMethod("getProperties").invoke(profile);
            properties.getClass().getMethod("put", Object.class, Object.class)
                    .invoke(properties, "textures", textures);

            final Field field = meta.getClass().getDeclaredField("profile");
            field.setAccessible(true);
            field.set(meta, profile);
        } catch (Exception ignored) {
        }
    }

    @Nullable
    private static String decodeSkinUrl(@NotNull String base64) {
        try {
            final String json = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
            final Matcher matcher = SKIN_URL.matcher(json);
            return matcher.find() ? matcher.group(1) : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    @NotNull
    private static Material resolveHead() {
        final Material modern = Material.matchMaterial("PLAYER_HEAD");
        return modern == null ? Material.STONE : modern;
    }
}
