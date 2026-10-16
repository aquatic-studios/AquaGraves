package com.aquaticstudios.aquagraves.grave;

import com.aquaticstudios.aquagraves.collection.LongIndex;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class GraveRegistry {
    private static final class WorldSlot {
        private final World world;
        private final LongIndex<Grave> graves = new LongIndex<>();

        private WorldSlot(World world) {
            this.world = world;
        }
    }

    private static final WorldSlot[] NO_WORLDS = new WorldSlot[0];

    private final Map<UUID, Grave> byId = new ConcurrentHashMap<>();
    private final Map<UUID, Grave> byEntity = new ConcurrentHashMap<>();
    private final Map<UUID, CopyOnWriteArrayList<Grave>> byOwner = new ConcurrentHashMap<>();

    private volatile WorldSlot[] worlds = NO_WORLDS;
    private volatile int size;
    private volatile int entityCount;

    public void add(@NotNull Grave grave) {
        byId.put(grave.id(), grave);

        final World world = grave.world();
        if (world != null) slotFor(world).graves.put(grave.blockKey(), grave);

        final CopyOnWriteArrayList<Grave> owned =
                byOwner.computeIfAbsent(grave.ownerId(), key -> new CopyOnWriteArrayList<>());

        synchronized (owned) {
            owned.add(insertionIndex(owned, grave), grave);
        }

        size = byId.size();
    }

    public void remove(@NotNull Grave grave) {
        byId.remove(grave.id(), grave);

        final World world = grave.world();
        if (world != null) {
            final WorldSlot slot = findSlot(world);
            if (slot != null) slot.graves.remove(grave.blockKey(), grave);
        }

        final CopyOnWriteArrayList<Grave> owned = byOwner.get(grave.ownerId());
        if (owned != null) {
            synchronized (owned) {
                owned.remove(grave);
                if (owned.isEmpty()) byOwner.remove(grave.ownerId(), owned);
            }
        }

        size = byId.size();
    }

    public void mapEntity(@NotNull UUID entityId, @NotNull Grave grave) {
        byEntity.put(entityId, grave);
        entityCount = byEntity.size();
    }

    public void unmapEntity(@NotNull UUID entityId) {
        byEntity.remove(entityId);
        entityCount = byEntity.size();
    }

    @Nullable
    public Grave byId(@NotNull UUID id) {
        return byId.get(id);
    }

    @Nullable
    public Grave byEntity(@NotNull UUID entityId) {
        if (entityCount == 0) return null;
        return byEntity.get(entityId);
    }

    @Nullable
    public Grave at(@NotNull World world, int x, int y, int z) {
        if (size == 0) return null;

        final WorldSlot slot = findSlot(world);
        if (slot == null) return null;

        return slot.graves.get(GraveKeys.block(x, y, z));
    }

    @Nullable
    public Grave at(@Nullable Block block) {
        if (block == null) return null;
        return at(block.getWorld(), block.getX(), block.getY(), block.getZ());
    }

    @Nullable
    public Grave at(@Nullable Location location) {
        if (location == null || location.getWorld() == null) return null;
        return at(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @NotNull
    public List<Grave> ofOwner(@NotNull UUID ownerId) {
        final List<Grave> owned = byOwner.get(ownerId);
        return owned == null ? Collections.emptyList() : owned;
    }

    public int countOfOwner(@NotNull UUID ownerId) {
        final List<Grave> owned = byOwner.get(ownerId);
        return owned == null ? 0 : owned.size();
    }

    @NotNull
    public Collection<Grave> all() {
        return byId.values();
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int ownerCount() {
        return byOwner.size();
    }

    @NotNull
    public List<Grave> near(@NotNull Location center, double radius) {
        final World world = center.getWorld();
        if (world == null || size == 0) return Collections.emptyList();

        final double radiusSquared = radius * radius;
        final List<Grave> found = new ArrayList<>();

        for (Grave grave : byId.values()) {
            if (grave.world() != world) continue;
            if (grave.distanceSquaredTo(center) > radiusSquared) continue;
            found.add(grave);
        }

        found.sort((left, right) -> Double.compare(
                left.distanceSquaredTo(center), right.distanceSquaredTo(center)));
        return found;
    }

    @NotNull
    public Set<UUID> owners() {
        return new HashSet<>(byOwner.keySet());
    }

    public void clear() {
        byId.clear();
        byEntity.clear();
        byOwner.clear();
        worlds = NO_WORLDS;
        size = 0;
        entityCount = 0;
    }

    private WorldSlot findSlot(World world) {
        final WorldSlot[] snapshot = worlds;
        for (int i = 0; i < snapshot.length; i++) {
            if (snapshot[i].world == world) return snapshot[i];
        }
        return null;
    }

    private synchronized WorldSlot slotFor(World world) {
        final WorldSlot existing = findSlot(world);
        if (existing != null) return existing;

        final WorldSlot slot = new WorldSlot(world);
        final WorldSlot[] snapshot = worlds;
        final WorldSlot[] grown = new WorldSlot[snapshot.length + 1];

        System.arraycopy(snapshot, 0, grown, 0, snapshot.length);
        grown[snapshot.length] = slot;
        worlds = grown;

        return slot;
    }

    private static int insertionIndex(@Nullable List<Grave> owned, Grave grave) {
        if (owned == null || owned.isEmpty()) return 0;

        for (int i = 0; i < owned.size(); i++) {
            if (owned.get(i).createdAt() <= grave.createdAt()) return i;
        }
        return owned.size();
    }
}
