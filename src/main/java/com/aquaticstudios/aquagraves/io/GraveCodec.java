package com.aquaticstudios.aquagraves.io;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.UUID;

public final class GraveCodec {
    static final char SEPARATOR = 0x1F;
    static final String HEADER = "# AquaGraves data file, one grave per line, do not edit by hand";
    private static final int FIELDS = 13;

    public static void encode(@NotNull StringBuilder out,
                              @NotNull UUID id,
                              @NotNull UUID owner,
                              @NotNull String ownerName,
                              @NotNull Location location,
                              long created,
                              long protectedUntil,
                              int xp,
                              @NotNull String contents) {
        out.append(id).append(SEPARATOR)
                .append(owner).append(SEPARATOR)
                .append(ownerName).append(SEPARATOR)
                .append(location.getWorld() == null ? "" : location.getWorld().getName()).append(SEPARATOR)
                .append(location.getX()).append(SEPARATOR)
                .append(location.getY()).append(SEPARATOR)
                .append(location.getZ()).append(SEPARATOR)
                .append(location.getYaw()).append(SEPARATOR)
                .append(location.getPitch()).append(SEPARATOR)
                .append(created).append(SEPARATOR)
                .append(protectedUntil).append(SEPARATOR)
                .append(xp).append(SEPARATOR)
                .append(contents);
    }

    @Nullable
    public static GraveRecord decode(@NotNull String line) {
        final String[] parts = split(line);
        if (parts == null) return null;

        try {
            return new GraveRecord(
                    UUID.fromString(parts[0]),
                    UUID.fromString(parts[1]),
                    parts[2],
                    parts[3],
                    Double.parseDouble(parts[4]),
                    Double.parseDouble(parts[5]),
                    Double.parseDouble(parts[6]),
                    Float.parseFloat(parts[7]),
                    Float.parseFloat(parts[8]),
                    Long.parseLong(parts[9]),
                    Long.parseLong(parts[10]),
                    Integer.parseInt(parts[11]),
                    parts[12],
                    line);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    @Nullable
    private static String[] split(String line) {
        final String[] parts = new String[FIELDS];

        int index = 0;
        int cursor = 0;

        while (index < FIELDS - 1) {
            final int next = line.indexOf(SEPARATOR, cursor);
            if (next < 0) return null;

            parts[index++] = line.substring(cursor, next);
            cursor = next + 1;
        }

        parts[FIELDS - 1] = line.substring(cursor);
        return parts;
    }

    @NotNull
    public static String encodeItems(@Nullable ItemStack[] contents) {
        if (contents == null || contents.length == 0) return "";

        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(1024)) {
            try (BukkitObjectOutputStream stream = new BukkitObjectOutputStream(bytes)) {
                stream.writeObject(contents);
            }
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialise grave contents", exception);
        }
    }

    @Nullable
    public static ItemStack[] decodeItems(@Nullable String data) {
        if (data == null || data.isEmpty()) return null;

        try (ByteArrayInputStream bytes = new ByteArrayInputStream(Base64.getDecoder().decode(data))) {
            try (BukkitObjectInputStream stream = new BukkitObjectInputStream(bytes)) {
                return (ItemStack[]) stream.readObject();
            }
        } catch (Exception exception) {
            return null;
        }
    }
}
