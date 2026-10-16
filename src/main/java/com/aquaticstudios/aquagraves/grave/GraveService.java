package com.aquaticstudios.aquagraves.grave;

import com.aquaticstudios.aquagraves.config.ConfigService;
import com.aquaticstudios.aquagraves.config.GraveConfig;
import com.aquaticstudios.aquagraves.config.MessageBundle;
import com.aquaticstudios.aquagraves.io.BackupRepository;
import com.aquaticstudios.aquagraves.io.GraveCodec;
import com.aquaticstudios.aquagraves.io.GraveRecord;
import com.aquaticstudios.aquagraves.io.GraveRepository;
import com.aquaticstudios.aquagraves.platform.PlatformScheduler;
import com.aquaticstudios.aquagraves.util.PermissionCache;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class GraveService implements GraveContext {
    private static final int LOAD_BATCH = 40;

    private final ConfigService configs;
    private final PlatformScheduler scheduler;
    private final GraveRegistry registry;
    private final GraveRepository repository;
    private final BackupRepository backups;
    private final PermissionCache permissions;
    private final Logger logger;

    private GraveTicker ticker;

    public GraveService(@NotNull ConfigService configs,
                        @NotNull PlatformScheduler scheduler,
                        @NotNull GraveRegistry registry,
                        @NotNull GraveRepository repository,
                        @NotNull BackupRepository backups,
                        @NotNull PermissionCache permissions,
                        @NotNull Logger logger) {
        this.configs = configs;
        this.scheduler = scheduler;
        this.registry = registry;
        this.repository = repository;
        this.backups = backups;
        this.permissions = permissions;
        this.logger = logger;
    }

    public void bind(@NotNull GraveTicker ticker) {
        this.ticker = ticker;
    }

    @NotNull
    @Override
    public GraveConfig config() {
        return configs.config();
    }

    @NotNull
    @Override
    public MessageBundle messages() {
        return configs.messages();
    }

    @NotNull
    @Override
    public PlatformScheduler scheduler() {
        return scheduler;
    }

    @NotNull
    @Override
    public GraveRegistry registry() {
        return registry;
    }

    @NotNull
    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public void contentsChanged(@NotNull Grave grave) {
        repository.markDirty();
    }

    @Override
    public void graveRemoved(@NotNull Grave grave, @NotNull RemoveReason reason) {
        final GraveConfig config = configs.config();

        if (config.backupsEnabled && reason.backup() && !grave.isEmpty()) {
            backups.store(grave, config);
        }

        registry.remove(grave);
        repository.markDirty();
    }

    @NotNull
    public Grave create(@NotNull Location location,
                        @NotNull UUID ownerId,
                        @Nullable String ownerName,
                        @NotNull List<ItemStack> items,
                        int storedXp,
                        long protectedUntil) {
        final GraveConfig config = configs.config();

        final Grave grave = new Grave(this, UUID.randomUUID(), location, ownerId, ownerName,
                items, storedXp, System.currentTimeMillis(), protectedUntil, null);

        registry.add(grave);

        grave.refreshEncoding();
        repository.markDirty();

        grave.spawnVisuals(GraveTick.immediate(config, configs.messages()));
        if (ticker != null) ticker.ensureRunning();

        return grave;
    }

    public int limitFor(@NotNull Player player) {
        return permissions.limit(player, configs.config().graveLimit);
    }

    public int protectionSecondsFor(@NotNull Player player) {
        return permissions.protectionSeconds(player, configs.config().protectionSeconds);
    }

    public long protectionDeadlineFor(@NotNull Player player) {
        final GraveConfig config = configs.config();
        if (!config.protectionEnabled) return 0L;

        final int seconds = protectionSecondsFor(player);
        if (seconds <= 0) return Long.MAX_VALUE;
        return System.currentTimeMillis() + seconds * 1000L;
    }

    public boolean makeRoom(@NotNull Player player) {
        final int limit = limitFor(player);
        if (limit < 0) return true;

        final List<Grave> owned = registry.ofOwner(player.getUniqueId());
        if (owned.size() < limit) return true;
        if (configs.config().limitReached == LimitAction.DENY) return false;

        final List<Grave> snapshot = new ArrayList<>(owned);
        for (int i = snapshot.size() - 1; i >= Math.max(0, limit - 1); i--) {
            snapshot.get(i).remove(RemoveReason.LIMIT);
        }
        return true;
    }

    public int removeAll(@NotNull RemoveReason reason) {
        final List<Grave> graves = new ArrayList<>(registry.all());
        for (int i = 0; i < graves.size(); i++) graves.get(i).remove(reason);
        return graves.size();
    }

    public int removeAllOf(@NotNull UUID ownerId, @NotNull RemoveReason reason) {
        final List<Grave> graves = new ArrayList<>(registry.ofOwner(ownerId));
        for (int i = 0; i < graves.size(); i++) graves.get(i).remove(reason);
        return graves.size();
    }

    @NotNull
    public CompletableFuture<Integer> loadAll() {
        final CompletableFuture<Integer> result = new CompletableFuture<>();

        repository.readAsync().thenAccept(records -> {
            if (records == null || records.isEmpty()) {
                result.complete(0);
                return;
            }

            try {
                scheduler.global(() -> materialise(records, 0, 0, result));
            } catch (IllegalStateException disabled) {
                result.complete(0);
            }
        });

        return result;
    }

    private void materialise(List<GraveRecord> records, int from, int loaded, CompletableFuture<Integer> result) {
        final GraveConfig config = configs.config();
        final GraveTick pass = GraveTick.immediate(config, configs.messages());

        int created = loaded;
        final int to = Math.min(records.size(), from + LOAD_BATCH);

        for (int i = from; i < to; i++) {
            try {
                if (build(records.get(i), pass)) created++;
            } catch (Exception exception) {
                logger.log(Level.WARNING, "Could not restore grave " + records.get(i).id, exception);
            }
        }

        if (to < records.size()) {
            final int nextLoaded = created;
            scheduler.globalLater(() -> materialise(records, to, nextLoaded, result), 1L);
            return;
        }

        logger.info("Restored " + created + " grave(s) from disk");
        if (ticker != null && created > 0) ticker.ensureRunning();
        result.complete(created);
    }

    private boolean build(GraveRecord record, GraveTick pass) {
        final World world = Bukkit.getWorld(record.world);
        if (world == null) {
            logger.warning("Grave " + record.id + " refers to unloaded world " + record.world + ", skipping it");
            return false;
        }

        final ItemStack[] contents = GraveCodec.decodeItems(record.contents);
        final List<ItemStack> items = contents == null
                ? Collections.emptyList()
                : Arrays.asList(contents);

        if (items.isEmpty() && record.xp <= 0) return false;

        final Location location = new Location(world, record.x, record.y, record.z, record.yaw, record.pitch);
        final Grave grave = new Grave(this, record.id, location, record.owner, record.ownerName,
                items, record.xp, record.created, record.protectedUntil, record.line);

        registry.add(grave);
        grave.spawnVisuals(pass);
        return true;
    }

    @NotNull
    public GraveRegistry graves() {
        return registry;
    }

    @NotNull
    public BackupRepository backups() {
        return backups;
    }

    @NotNull
    public GraveRepository repository() {
        return repository;
    }

    @NotNull
    public PermissionCache permissions() {
        return permissions;
    }

    @NotNull
    public ConfigService configs() {
        return configs;
    }
}
