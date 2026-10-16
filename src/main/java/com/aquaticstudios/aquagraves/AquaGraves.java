package com.aquaticstudios.aquagraves;

import com.aquaticstudios.aquagraves.api.AquaGravesAPI;
import com.aquaticstudios.aquagraves.api.AquaGravesAPIImpl;
import com.aquaticstudios.aquagraves.commands.CommandContext;
import com.aquaticstudios.aquagraves.commands.CommandRegistrar;
import com.aquaticstudios.aquagraves.config.ConfigService;
import com.aquaticstudios.aquagraves.grave.Grave;
import com.aquaticstudios.aquagraves.grave.GraveEntities;
import com.aquaticstudios.aquagraves.grave.GraveRegistry;
import com.aquaticstudios.aquagraves.grave.GraveService;
import com.aquaticstudios.aquagraves.grave.GraveTicker;
import com.aquaticstudios.aquagraves.hologram.HologramFactory;
import com.aquaticstudios.aquagraves.hook.PlaceholderHook;
import com.aquaticstudios.aquagraves.io.BackupRepository;
import com.aquaticstudios.aquagraves.io.GraveRepository;
import com.aquaticstudios.aquagraves.io.IoWorker;
import com.aquaticstudios.aquagraves.listeners.DeathListener;
import com.aquaticstudios.aquagraves.listeners.GraveInteractListener;
import com.aquaticstudios.aquagraves.listeners.GraveInventoryListener;
import com.aquaticstudios.aquagraves.listeners.PlayerSessionListener;
import com.aquaticstudios.aquagraves.platform.Platform;
import com.aquaticstudios.aquagraves.platform.PlatformScheduler;
import com.aquaticstudios.aquagraves.platform.SchedulerFactory;
import com.aquaticstudios.aquagraves.player.PlayerLookup;
import com.aquaticstudios.aquagraves.player.TeleportService;
import com.aquaticstudios.aquagraves.util.CC;
import com.aquaticstudios.aquagraves.util.PermissionCache;
import com.aquaticstudios.aquagraves.util.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class AquaGraves extends JavaPlugin {
    private static final int BSTATS_PLUGIN_ID = 34728;
    private static final long SHUTDOWN_TIMEOUT_MILLIS = 5_000L;

    private ConfigService configs;
    private PlatformScheduler scheduler;
    private IoWorker io;

    private GraveRegistry registry;
    private GraveRepository repository;
    private BackupRepository backups;
    private GraveService graves;
    private GraveTicker ticker;

    private TeleportService teleports;
    private PermissionCache permissions;
    private UpdateChecker updates;
    private CommandRegistrar commands;

    @Override
    public void onEnable() {
        if (!isSupported()) {
            getLogger().severe("AquaGraves needs Minecraft 1.16 or newer. Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        configs = new ConfigService(this);
        try {
            configs.load();
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "Could not read the configuration, disabling", exception);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        scheduler = SchedulerFactory.create(this);
        io = new IoWorker("AquaGraves-IO", getLogger());

        registry = new GraveRegistry();
        permissions = new PermissionCache();

        repository = new GraveRepository(
                getDataFolder().toPath().resolve("graves.dat"), io, registry, scheduler, configs, getLogger());
        backups = new BackupRepository(
                getDataFolder().toPath().resolve("backups.dat"), io, getLogger());

        graves = new GraveService(configs, scheduler, registry, repository, backups, permissions, getLogger());
        ticker = new GraveTicker(configs, scheduler, registry, repository, getLogger());
        graves.bind(ticker);

        teleports = new TeleportService(configs, scheduler);
        updates = new UpdateChecker(this, configs);

        registerListeners();
        registerCommands();
        registerServices();

        backups.loadAsync();
        graves.loadAll().thenAccept(loaded -> scheduler.global(this::afterLoad));

        updates.check();

        Bukkit.getConsoleSender().sendMessage(CC.legacy("&#33CCFF[AquaGraves] Enabled on &f"
                + Platform.current().name().toLowerCase()
                + "&#33CCFF, holograms: &f" + HologramFactory.describe(configs.config())
                + "&#33CCFF, loading graves in the background"));
    }

    private void afterLoad() {
        if (!isEnabled()) return;

        if (!Platform.isFolia()) {
            final int orphans = GraveEntities.cleanupLoaded(registry);
            if (orphans > 0) getLogger().info("Removed " + orphans + " leftover grave entity/entities");
        }

        ticker.ensureRunning();
    }

    @Override
    public void onDisable() {
        if (ticker != null) ticker.stop();

        if (registry != null) {
            for (Grave grave : registry.all()) {
                try {
                    grave.closeViewers(null);
                } catch (Exception ignored) {
                }
            }
        }

        if (repository != null) repository.flushBlocking();
        if (backups != null) backups.flushBlocking();

        if (scheduler != null) scheduler.shutdown();
        if (io != null) io.shutdown(SHUTDOWN_TIMEOUT_MILLIS);

        if (registry != null) {
            for (Grave grave : registry.all()) {
                try {
                    grave.despawnEntities();
                } catch (Exception ignored) {
                }
            }
            registry.clear();
        }

        if (commands != null) commands.unregister();
        if (teleports != null) teleports.clear();
        if (permissions != null) permissions.clear();
        if (backups != null) backups.clear();

        getServer().getServicesManager().unregisterAll(this);

        configs = null;
        scheduler = null;
        io = null;
        registry = null;
        repository = null;
        backups = null;
        graves = null;
        ticker = null;
        teleports = null;
        permissions = null;
        updates = null;
        commands = null;
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new GraveInteractListener(registry), this);
        getServer().getPluginManager().registerEvents(new GraveInventoryListener(this, configs, scheduler), this);
        getServer().getPluginManager().registerEvents(
                new PlayerSessionListener(configs, teleports, permissions, updates), this);

        new DeathListener(this, configs, graves, getLogger()).register();
    }

    private void registerCommands() {
        final PlayerLookup lookup = new PlayerLookup(registry, configs.messages().unknownPlayer);
        final CommandContext context = new CommandContext(this, configs, graves, teleports, lookup, ticker);

        commands = new CommandRegistrar(this);
        commands.register(context, configs.config().commandAliases);
    }

    private void registerServices() {
        final AquaGravesAPI api = new AquaGravesAPIImpl(graves);
        getServer().getServicesManager().register(AquaGravesAPI.class, api, this, ServicePriority.Normal);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) hookPlaceholders();

        if (BSTATS_PLUGIN_ID > 0) new org.bstats.bukkit.Metrics(this, BSTATS_PLUGIN_ID);
    }

    private void hookPlaceholders() {
        new PlaceholderHook(this, configs, graves).register();
        getLogger().info("Hooked into PlaceholderAPI");
    }

    private boolean isSupported() {
        final String[] parts = Bukkit.getBukkitVersion().split("-")[0].split("\\.");
        if (parts.length < 2) return true;

        try {
            final int major = Integer.parseInt(parts[0]);
            final int minor = Integer.parseInt(parts[1]);
            return major > 1 || minor >= 16;
        } catch (NumberFormatException exception) {
            return true;
        }
    }
}
