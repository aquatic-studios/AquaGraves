package com.aquaticstudios.aquagraves.io;

import com.aquaticstudios.aquagraves.config.GraveConfig;
import com.aquaticstudios.aquagraves.grave.Grave;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BackupRepository {
    private static final char SEPARATOR = 0x1F;
    private static final String HEADER = "# AquaGraves backups, one entry per line, do not edit by hand";
    private static final int FIELDS = 8;

    public static final class Backup {
        public final UUID owner;
        public final long removedAt;
        public final String world;
        public final int x;
        public final int y;
        public final int z;
        public final int xp;
        private final String contents;

        Backup(UUID owner, long removedAt, String world, int x, int y, int z, int xp, String contents) {
            this.owner = owner;
            this.removedAt = removedAt;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.xp = xp;
            this.contents = contents;
        }

        @Nullable
        public ItemStack[] items() {
            return GraveCodec.decodeItems(contents);
        }

        void encode(StringBuilder out) {
            out.append(owner).append(SEPARATOR)
                    .append(removedAt).append(SEPARATOR)
                    .append(world).append(SEPARATOR)
                    .append(x).append(SEPARATOR)
                    .append(y).append(SEPARATOR)
                    .append(z).append(SEPARATOR)
                    .append(xp).append(SEPARATOR)
                    .append(contents);
        }
    }

    private final Map<UUID, List<Backup>> byOwner = new ConcurrentHashMap<>();
    private final DataFile file;
    private final IoWorker io;
    private final Logger logger;
    private final AtomicBoolean dirty = new AtomicBoolean();

    public BackupRepository(@NotNull Path path, @NotNull IoWorker io, @NotNull Logger logger) {
        this.file = new DataFile(path);
        this.io = io;
        this.logger = logger;
    }

    public void store(@NotNull Grave grave, @NotNull GraveConfig config) {
        if (!config.backupsEnabled) return;

        final Location location = grave.locationRef();
        final Backup backup = new Backup(
                grave.ownerId(),
                System.currentTimeMillis(),
                location.getWorld() == null ? "?" : location.getWorld().getName(),
                grave.blockX(), grave.blockY(), grave.blockZ(),
                grave.storedXp(),
                GraveCodec.encodeItems(grave.snapshot()));

        final List<Backup> stored = byOwner.computeIfAbsent(grave.ownerId(), key -> new ArrayList<>(4));
        synchronized (stored) {
            stored.add(0, backup);
            while (stored.size() > config.backupsPerPlayer) stored.remove(stored.size() - 1);
        }

        dirty.set(true);
    }

    @NotNull
    public List<Backup> of(@NotNull UUID ownerId) {
        final List<Backup> stored = byOwner.get(ownerId);
        if (stored == null) return Collections.emptyList();

        synchronized (stored) {
            return new ArrayList<>(stored);
        }
    }

    @Nullable
    public Backup take(@NotNull UUID ownerId, int index) {
        final List<Backup> stored = byOwner.get(ownerId);
        if (stored == null) return null;

        final Backup taken;
        synchronized (stored) {
            if (index < 0 || index >= stored.size()) return null;

            taken = stored.remove(index);
            if (stored.isEmpty()) byOwner.remove(ownerId, stored);
        }

        dirty.set(true);
        return taken;
    }

    public int total() {
        int amount = 0;
        for (List<Backup> stored : byOwner.values()) {
            synchronized (stored) {
                amount += stored.size();
            }
        }
        return amount;
    }

    public void clear() {
        byOwner.clear();
        dirty.set(true);
    }

    @NotNull
    public CompletableFuture<Void> loadAsync() {
        return io.run(() -> {
            try {
                for (String line : file.readLines()) {
                    final Backup backup = decode(line);
                    if (backup == null) continue;

                    byOwner.computeIfAbsent(backup.owner, key -> new ArrayList<>(4)).add(backup);
                }
            } catch (Exception exception) {
                logger.log(Level.SEVERE, "Could not read the backup file", exception);
            }
        });
    }

    public void flushIfDirty() {
        if (!dirty.compareAndSet(true, false)) return;
        final StringBuilder out = build();
        io.run(() -> write(out));
    }

    public void flushBlocking() {
        dirty.set(false);
        write(build());
    }

    private StringBuilder build() {
        final StringBuilder out = new StringBuilder(512);
        out.append(HEADER).append('\n');

        for (List<Backup> stored : byOwner.values()) {
            synchronized (stored) {
                for (int i = 0; i < stored.size(); i++) {
                    stored.get(i).encode(out);
                    out.append('\n');
                }
            }
        }
        return out;
    }

    private void write(CharSequence content) {
        try {
            file.write(content);
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Could not write the backup file", exception);
        }
    }

    @Nullable
    private static Backup decode(String line) {
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

        try {
            return new Backup(
                    UUID.fromString(parts[0]),
                    Long.parseLong(parts[1]),
                    parts[2],
                    Integer.parseInt(parts[3]),
                    Integer.parseInt(parts[4]),
                    Integer.parseInt(parts[5]),
                    Integer.parseInt(parts[6]),
                    parts[7]);
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
