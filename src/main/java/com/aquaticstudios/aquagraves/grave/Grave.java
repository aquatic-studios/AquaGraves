package com.aquaticstudios.aquagraves.grave;

import com.aquaticstudios.aquagraves.api.event.GraveLootEvent;
import com.aquaticstudios.aquagraves.api.event.GraveOpenEvent;
import com.aquaticstudios.aquagraves.api.event.GraveUnlockEvent;
import com.aquaticstudios.aquagraves.config.GraveConfig;
import com.aquaticstudios.aquagraves.config.MessageBundle;
import com.aquaticstudios.aquagraves.config.template.GraveField;
import com.aquaticstudios.aquagraves.hologram.Hologram;
import com.aquaticstudios.aquagraves.hologram.HologramFactory;
import com.aquaticstudios.aquagraves.io.GraveCodec;
import com.aquaticstudios.aquagraves.util.ExperienceUtil;
import com.aquaticstudios.aquagraves.util.InventoryUtil;
import com.aquaticstudios.aquagraves.util.ItemFilter;
import com.aquaticstudios.aquagraves.util.Permissions;
import com.aquaticstudios.aquagraves.util.SkullUtil;
import com.aquaticstudios.aquagraves.util.Sounds;
import com.aquaticstudios.aquagraves.util.Worlds;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Grave implements GraveField.Source {
    private final GraveContext context;
    private final UUID id;
    private final UUID ownerId;
    private final String ownerName;
    private final Location location;
    private final World world;
    private final int blockX;
    private final int blockY;
    private final int blockZ;
    private final int chunkX;
    private final int chunkZ;
    private final long blockKey;
    private final long worldSeed;
    private final long createdAt;
    private final Inventory inventory;

    private volatile long protectedUntil;
    private volatile int storedXp;
    private volatile boolean unlockAnnounced;
    private final AtomicBoolean removed = new AtomicBoolean();

    private volatile int itemCount;
    private volatile int stackCount;
    private volatile boolean countsStale = true;

    private volatile String encodedLine;
    private volatile boolean encodingStale;

    private ArmorStand head;
    private Hologram hologram;

    public Grave(@NotNull GraveContext context,
                 @NotNull UUID id,
                 @NotNull Location origin,
                 @NotNull UUID ownerId,
                 @Nullable String ownerName,
                 @NotNull List<ItemStack> items,
                 int storedXp,
                 long createdAt,
                 long protectedUntil,
                 @Nullable String encodedLine) {
        final GraveConfig config = context.config();

        this.context = context;
        this.id = id;
        this.ownerId = ownerId;
        this.ownerName = ownerName == null || ownerName.isEmpty()
                ? context.messages().unknownPlayer
                : ownerName;
        this.createdAt = createdAt;
        this.storedXp = Math.max(0, storedXp);
        this.protectedUntil = protectedUntil;

        this.location = Worlds.center(origin);
        Worlds.clampHeight(this.location, config);

        this.world = this.location.getWorld();
        this.blockX = this.location.getBlockX();
        this.blockY = this.location.getBlockY();
        this.blockZ = this.location.getBlockZ();
        this.chunkX = blockX >> 4;
        this.chunkZ = blockZ >> 4;
        this.blockKey = GraveKeys.block(blockX, blockY, blockZ);
        this.worldSeed = world == null ? 0L : world.getUID().getLeastSignificantBits();

        this.unlockAnnounced = protectedUntil != 0L
                && protectedUntil != Long.MAX_VALUE
                && System.currentTimeMillis() >= protectedUntil;

        final List<ItemStack> accepted = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            final ItemStack item = items.get(i);
            if (InventoryUtil.isEmpty(item)) continue;
            if (ItemFilter.isDenied(item, config)) continue;
            accepted.add(item.clone());
        }

        final GraveHolder holder = new GraveHolder();
        this.inventory = Bukkit.createInventory(holder,
                InventoryUtil.requiredRows(accepted.size()) * 9,
                context.messages().guiTitle.replace("%player%", this.ownerName));
        holder.bind(this, this.inventory);

        for (int i = 0; i < accepted.size(); i++) {
            final Map<Integer, ItemStack> overflow = inventory.addItem(accepted.get(i));
            if (overflow.isEmpty()) continue;

            for (ItemStack leftover : overflow.values()) {
                InventoryUtil.drop(location, leftover, config.droppedItemVelocity);
            }
        }

        this.encodedLine = encodedLine;
        this.encodingStale = encodedLine == null;
        refreshCounts();
    }

    @NotNull
    public UUID id() {
        return id;
    }

    @NotNull
    public UUID ownerId() {
        return ownerId;
    }

    @NotNull
    public String ownerName() {
        return ownerName;
    }

    @NotNull
    public OfflinePlayer owner() {
        return Bukkit.getOfflinePlayer(ownerId);
    }

    @Nullable
    public World world() {
        return world;
    }

    @NotNull
    public Location locationRef() {
        return location;
    }

    @NotNull
    public Location location() {
        return location.clone();
    }

    public long blockKey() {
        return blockKey;
    }

    public int blockX() {
        return blockX;
    }

    public int blockY() {
        return blockY;
    }

    public int blockZ() {
        return blockZ;
    }

    public long createdAt() {
        return createdAt;
    }

    @NotNull
    public Inventory inventory() {
        return inventory;
    }

    public int storedXp() {
        return storedXp;
    }

    public boolean isRemoved() {
        return removed.get();
    }

    public boolean isOwner(@NotNull HumanEntity entity) {
        return entity.getUniqueId().equals(ownerId);
    }

    public double distanceSquaredTo(@NotNull Location other) {
        if (other.getWorld() != world) return Double.MAX_VALUE;

        final double dx = other.getX() - location.getX();
        final double dy = other.getY() - location.getY();
        final double dz = other.getZ() - location.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    public long expiresAt() {
        final long despawn = context.config().despawnMillis;
        return despawn == Long.MAX_VALUE ? Long.MAX_VALUE : createdAt + despawn;
    }

    public long remainingMillis() {
        return remainingMillis(System.currentTimeMillis());
    }

    public long remainingMillis(long now) {
        final long expires = expiresAt();
        if (expires == Long.MAX_VALUE) return Long.MAX_VALUE;
        return Math.max(0L, expires - now);
    }

    public long protectedUntil() {
        return protectedUntil;
    }

    public boolean isProtected() {
        return isProtected(System.currentTimeMillis());
    }

    public boolean isProtected(long now) {
        if (!context.config().protectionEnabled) return false;
        if (protectedUntil == 0L) return false;
        return protectedUntil == Long.MAX_VALUE || now < protectedUntil;
    }

    public long protectionMillis(long now) {
        if (!isProtected(now)) return 0L;
        if (protectedUntil == Long.MAX_VALUE) return Long.MAX_VALUE;
        return Math.max(0L, protectedUntil - now);
    }

    public void protectedUntil(long value) {
        this.protectedUntil = value;
        context.contentsChanged(this);
    }

    public int itemCount() {
        if (countsStale) refreshCounts();
        return itemCount;
    }

    public int stackCount() {
        if (countsStale) refreshCounts();
        return stackCount;
    }

    public boolean isEmpty() {
        return itemCount() == 0 && storedXp <= 0;
    }

    public void markChanged() {
        countsStale = true;
        encodingStale = true;
        context.contentsChanged(this);
    }

    private void refreshCounts() {
        int items = 0;
        int stacks = 0;

        final ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            final ItemStack item = contents[i];
            if (item == null || item.getAmount() <= 0 || item.getType().isAir()) continue;

            items++;
            stacks += item.getAmount();
        }

        this.itemCount = items;
        this.stackCount = stacks;
        this.countsStale = false;
    }

    @NotNull
    public ItemStack[] snapshot() {
        synchronized (this) {
            final ItemStack[] contents = inventory.getContents();
            final ItemStack[] copy = new ItemStack[contents.length];
            for (int i = 0; i < contents.length; i++) {
                copy[i] = contents[i] == null ? null : contents[i].clone();
            }
            return copy;
        }
    }

    @Nullable
    public String encodedLine() {
        return encodedLine;
    }

    public boolean isEncodingStale() {
        return encodingStale;
    }

    @NotNull
    public String refreshEncoding() {
        final StringBuilder out = new StringBuilder(512);

        synchronized (this) {
            GraveCodec.encode(out, id, ownerId, ownerName, location, createdAt, protectedUntil,
                    storedXp, GraveCodec.encodeItems(inventory.getContents()));
        }

        final String line = out.toString();
        this.encodedLine = line;
        this.encodingStale = false;
        return line;
    }

    @Nullable
    public String denyReason(@NotNull Player player, long now) {
        if (isOwner(player)) return null;
        if (Permissions.canBypass(player, "protection")) return null;

        if (isProtected(now)) return "messages.grave-protected";
        if (context.config().alwaysOwnerOnly) return "messages.grave-owner-only";
        return null;
    }

    public boolean canAccess(@NotNull Player player) {
        return denyReason(player, System.currentTimeMillis()) == null;
    }

    public void interact(@NotNull Player player, boolean mainHand) {
        final GraveConfig config = context.config();
        final MessageBundle messages = context.messages();

        if (isRemoved()) {
            messages.send(player, "messages.grave-gone");
            return;
        }

        if (distanceSquaredTo(player.getLocation()) > config.maxInteractDistanceSquared) {
            messages.send(player, "messages.grave-too-far");
            return;
        }

        final String deny = denyReason(player, System.currentTimeMillis());
        if (deny != null) {
            messages.sendGrave(player, deny, this);
            return;
        }

        if (config.xpOnInteract) claimXp(player);

        if (mainHand && player.isSneaking() && config.instantPickup) {
            if (player.getGameMode() == GameMode.SPECTATOR) return;
            if (config.instantPickupOnlyOwn && !isOwner(player) && !Permissions.isAdmin(player)) return;

            loot(player);
            return;
        }

        open(player);
    }

    public void open(@NotNull Player player) {
        if (isRemoved()) {
            context.messages().send(player, "messages.grave-gone");
            return;
        }

        final GraveOpenEvent event = new GraveOpenEvent(player, this);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        player.openInventory(inventory);
        Sounds.play(player, context.config().soundOpen, context.config());
    }

    public void claimXp(@NotNull Player player) {
        final int amount;
        synchronized (this) {
            if (storedXp <= 0) return;
            amount = storedXp;
            storedXp = 0;
        }

        ExperienceUtil.changeExp(player, amount);
        context.messages().send(player, "messages.xp-restored", "%xp%", Integer.toString(amount));
        markChanged();
    }

    public void loot(@NotNull Player player) {
        if (isRemoved()) return;

        final GraveLootEvent event = new GraveLootEvent(player, this);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        final GraveConfig config = context.config();
        boolean overflow = false;

        synchronized (this) {
            closeViewers(player);

            final PlayerInventory target = player.getInventory();
            for (int slot = 0; slot < inventory.getSize(); slot++) {
                final ItemStack item = inventory.getItem(slot);
                if (InventoryUtil.isEmpty(item)) continue;

                inventory.setItem(slot, null);

                if (config.autoEquipArmor && equip(target, item)) continue;
                if (InventoryUtil.addOrShrink(target, item)) continue;

                inventory.setItem(slot, item);
                overflow = true;
            }
        }

        claimXp(player);
        Sounds.play(player, config.soundPickup, config);
        markChanged();

        if (overflow) context.messages().send(player, "messages.grave-full");
        else context.messages().send(player, "messages.grave-emptied", "%player%", ownerName);

        tick(GraveTick.immediate(config, context.messages()));
    }

    private boolean equip(@NotNull PlayerInventory target, @NotNull ItemStack item) {
        if (InventoryUtil.isHelmet(item.getType()) && InventoryUtil.isEmpty(target.getHelmet())) {
            target.setHelmet(item);
            return true;
        }
        if (InventoryUtil.isChestplate(item.getType()) && InventoryUtil.isEmpty(target.getChestplate())) {
            target.setChestplate(item);
            return true;
        }
        if (InventoryUtil.isLeggings(item.getType()) && InventoryUtil.isEmpty(target.getLeggings())) {
            target.setLeggings(item);
            return true;
        }
        if (InventoryUtil.isBoots(item.getType()) && InventoryUtil.isEmpty(target.getBoots())) {
            target.setBoots(item);
            return true;
        }
        return false;
    }

    public void closeViewers(@Nullable HumanEntity except) {
        final List<HumanEntity> viewers = inventory.getViewers();
        if (viewers.isEmpty()) return;

        for (HumanEntity viewer : new ArrayList<>(viewers)) {
            if (viewer == except) continue;
            viewer.closeInventory();
        }
    }

    public boolean hasViewers() {
        return !inventory.getViewers().isEmpty();
    }

    public void tick(@NotNull GraveTick pass) {
        if (isRemoved()) return;

        if (remainingMillis(pass.now) <= 0L) {
            remove(RemoveReason.EXPIRED);
            return;
        }

        if (!unlockAnnounced
                && pass.config.protectionEnabled
                && protectedUntil != 0L
                && protectedUntil != Long.MAX_VALUE
                && pass.now >= protectedUntil) {
            unlock(pass);
            if (isRemoved()) return;
        }

        if (countsStale) refreshCounts();

        if (pass.config.despawnWhenEmpty && itemCount == 0 && storedXp <= 0 && !hasViewers()) {
            remove(RemoveReason.EMPTIED);
            return;
        }

        if (pass.config.closeGuiWhenFar) closeDistantViewers(pass.config);

        refreshVisuals(pass);
    }

    private void unlock(GraveTick pass) {
        unlockAnnounced = true;

        if (pass.config.protectionExpire == ProtectionAction.REMOVE) {
            remove(RemoveReason.PROTECTION_EXPIRED);
            return;
        }

        Bukkit.getPluginManager().callEvent(new GraveUnlockEvent(this));
        Sounds.playAt(location, pass.config.soundUnlock, pass.config);

        if (!pass.config.protectionNotify) return;

        final Player owner = Bukkit.getPlayer(ownerId);
        if (owner != null) pass.messages.sendGrave(owner, "messages.grave-unlocked", this);
    }

    private void closeDistantViewers(GraveConfig config) {
        final List<HumanEntity> viewers = inventory.getViewers();
        if (viewers.isEmpty()) return;

        for (HumanEntity viewer : new ArrayList<>(viewers)) {
            if (distanceSquaredTo(viewer.getLocation()) <= config.maxInteractDistanceSquared) continue;
            viewer.closeInventory();
        }
    }

    public void refreshVisuals(@NotNull GraveTick pass) {
        if (isRemoved()) return;
        if (world == null || !world.isChunkLoaded(chunkX, chunkZ)) return;

        if (head == null || !head.isValid()) spawnHead(pass.config);
        else if (pass.config.autoRotation) rotateHead(pass.config);

        if (!pass.config.hologramEnabled) {
            if (hologram != null) {
                hologram.remove();
                hologram = null;
            }
            return;
        }

        if (!pass.isVisible(worldSeed, chunkX, chunkZ)) return;

        if (hologram == null || !hologram.isSpawned()) {
            hologram = HologramFactory.create(id, pass.config);
            hologram.spawn(hologramLocation(pass.config), this, pass.messages.hologramLines);
            return;
        }

        hologram.update(this, pass.messages.hologramLines);
    }

    public void spawnVisuals(@NotNull GraveTick pass) {
        context.scheduler().execute(location, () -> refreshVisuals(pass));
    }

    private Location hologramLocation(GraveConfig config) {
        return location.clone().add(0.0D, 1.0D + config.hologramHeight, 0.0D);
    }

    private void spawnHead(GraveConfig config) {
        if (world == null) return;

        if (head != null) {
            context.registry().unmapEntity(head.getUniqueId());
            head.remove();
            head = null;
        }

        final Location at = location.clone().add(0.0D, 1.0D + config.headHeight, 0.0D);
        at.setYaw(config.rotateHead360 ? location.getYaw() : Worlds.nearestDirection(location.getYaw()));

        final ItemStack skull = SkullUtil.graveSkull(owner(), config);

        head = world.spawn(at, ArmorStand.class, stand -> {
            stand.setSmall(true);
            stand.setVisible(false);
            stand.setBasePlate(false);
            stand.setArms(false);
            stand.setGravity(false);
            stand.setSilent(true);
            stand.setInvulnerable(true);
            stand.setCanPickupItems(false);
            stand.setCollidable(false);
            stand.setPersistent(false);
            stand.setRemoveWhenFarAway(false);

            final EntityEquipment equipment = stand.getEquipment();
            if (equipment != null) equipment.setHelmet(skull);

            GraveEntities.tag(stand, id);
        });

        context.registry().mapEntity(head.getUniqueId(), this);
    }

    private void rotateHead(GraveConfig config) {
        final Location at = head.getLocation();
        at.setYaw(at.getYaw() + config.autoRotationSpeed);
        head.teleport(at);
    }

    public void remove(@NotNull RemoveReason reason) {
        if (!removed.compareAndSet(false, true)) return;

        context.scheduler().execute(location, () -> {
            final GraveConfig config = context.config();

            closeViewers(null);

            context.graveRemoved(this, reason);

            if (reason.drops() && config.dropItemsOnDespawn) dropContents(config);
            else clearContents();

            despawnEntities();

            if (reason == RemoveReason.EXPIRED) {
                final Player owner = Bukkit.getPlayer(ownerId);
                if (owner != null) context.messages().sendGrave(owner, "messages.grave-expired", this);
            }

            if (reason != RemoveReason.EMPTIED) Sounds.playAt(location, config.soundDespawn, config);
        });
    }

    private void dropContents(GraveConfig config) {
        if (world == null) {
            clearContents();
            return;
        }

        synchronized (this) {
            for (int slot = 0; slot < inventory.getSize(); slot++) {
                final ItemStack item = inventory.getItem(slot);
                if (InventoryUtil.isEmpty(item)) continue;

                inventory.setItem(slot, null);
                InventoryUtil.drop(location, item, config.droppedItemVelocity);
            }

            if (storedXp > 0) {
                final int amount = storedXp;
                storedXp = 0;
                world.spawn(location, ExperienceOrb.class, orb -> orb.setExperience(amount));
            }
        }

        countsStale = true;
    }

    private void clearContents() {
        synchronized (this) {
            inventory.clear();
            storedXp = 0;
        }
        countsStale = true;
    }

    public void despawnEntities() {
        if (head != null) {
            context.registry().unmapEntity(head.getUniqueId());
            head.remove();
            head = null;
        }
        if (hologram != null) {
            hologram.remove();
            hologram = null;
        }
    }

    @Override
    public void appendField(int field, @NotNull StringBuilder out) {
        final MessageBundle messages = context.messages();
        final long now = System.currentTimeMillis();

        switch (field) {
            case GraveField.PLAYER:
                out.append(ownerName);
                break;
            case GraveField.ITEMS:
                out.append(itemCount());
                break;
            case GraveField.STACKS:
                out.append(stackCount());
                break;
            case GraveField.XP:
                out.append(storedXp);
                break;
            case GraveField.DESPAWN_TIME: {
                final long remaining = remainingMillis(now);
                if (remaining == Long.MAX_VALUE) out.append(messages.hologramForever);
                else messages.time.append(out, remaining);
                break;
            }
            case GraveField.PROTECTION_TIME: {
                final long remaining = protectionMillis(now);
                if (remaining == Long.MAX_VALUE) out.append(messages.hologramForever);
                else messages.time.append(out, remaining);
                break;
            }
            case GraveField.LOCK_STATUS:
                if (isProtected(now)) messages.hologramLocked.render(this, out);
                else out.append(messages.hologramUnlocked);
                break;
            case GraveField.AGE:
                messages.time.append(out, now - createdAt);
                break;
            case GraveField.VIEWERS:
                out.append(inventory.getViewers().size());
                break;
            case GraveField.WORLD:
                out.append(world == null ? "---" : context.config().worldName(world.getName()));
                break;
            case GraveField.X:
                out.append(blockX);
                break;
            case GraveField.Y:
                out.append(blockY);
                break;
            case GraveField.Z:
                out.append(blockZ);
                break;
            case GraveField.ID:
                out.append(id.toString(), 0, 8);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Grave)) return false;
        return id.equals(((Grave) other).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Grave(" + id + " of " + ownerName + ")";
    }
}
