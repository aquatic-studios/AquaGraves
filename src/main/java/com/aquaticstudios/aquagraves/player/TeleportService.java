package com.aquaticstudios.aquagraves.player;

import com.aquaticstudios.aquagraves.config.ConfigService;
import com.aquaticstudios.aquagraves.config.GraveConfig;
import com.aquaticstudios.aquagraves.platform.PlatformScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TeleportService {
    private final ConfigService configs;
    private final PlatformScheduler scheduler;

    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();
    private final Cooldowns cooldowns = new Cooldowns();

    private volatile int pendingCount;

    private static Method teleportAsync;
    private static boolean asyncChecked;

    private static final class Pending {
        private final int originX;
        private final int originY;
        private final int originZ;
        private final UUID originWorld;
        private final Location target;
        private volatile boolean cancelled;

        private Pending(Location origin, Location target) {
            this.originX = origin.getBlockX();
            this.originY = origin.getBlockY();
            this.originZ = origin.getBlockZ();
            this.originWorld = origin.getWorld() == null ? null : origin.getWorld().getUID();
            this.target = target;
        }
    }

    public TeleportService(@NotNull ConfigService configs, @NotNull PlatformScheduler scheduler) {
        this.configs = configs;
        this.scheduler = scheduler;
    }

    @NotNull
    public Cooldowns cooldowns() {
        return cooldowns;
    }

    public boolean hasPending() {
        return pendingCount != 0;
    }

    public boolean isPending(@NotNull UUID uuid) {
        return pendingCount != 0 && pending.containsKey(uuid);
    }

    public void request(@NotNull Player player, @NotNull Location target) {
        final GraveConfig config = configs.config();

        if (config.teleportWarmupSeconds <= 0) {
            finish(player, target, config);
            return;
        }

        final UUID uuid = player.getUniqueId();
        cancel(uuid, false);

        final Pending request = new Pending(player.getLocation(), target);
        pending.put(uuid, request);
        pendingCount = pending.size();

        configs.messages().send(player, "messages.teleport-warmup",
                "%seconds%", Integer.toString(config.teleportWarmupSeconds));

        scheduler.executeLater(target, () -> {
            if (request.cancelled) return;
            if (!pending.remove(uuid, request)) return;
            pendingCount = pending.size();

            final Player online = Bukkit.getPlayer(uuid);
            if (online == null || !online.isOnline()) return;

            finish(online, request.target, configs.config());
        }, config.teleportWarmupTicks);
    }

    private void finish(Player player, Location target, GraveConfig config) {
        cooldowns.apply(player.getUniqueId(), config.teleportCooldownMillis);
        teleport(player, target);

        configs.messages().send(player, "messages.teleport-done",
                "%world%", config.worldName(target.getWorld() == null ? "?" : target.getWorld().getName()),
                "%x%", Integer.toString(target.getBlockX()),
                "%y%", Integer.toString(target.getBlockY()),
                "%z%", Integer.toString(target.getBlockZ()));
    }

    public void cancel(@NotNull UUID uuid, boolean notify) {
        final Pending request = pending.remove(uuid);
        if (request == null) return;

        request.cancelled = true;
        pendingCount = pending.size();

        if (!notify) return;

        final Player player = Bukkit.getPlayer(uuid);
        if (player != null) configs.messages().send(player, "messages.teleport-cancelled");
    }

    public void onMove(@NotNull Player player, @NotNull Location to) {
        final Pending request = pending.get(player.getUniqueId());
        if (request == null) return;

        if (to.getBlockX() == request.originX
                && to.getBlockY() == request.originY
                && to.getBlockZ() == request.originZ
                && to.getWorld() != null
                && to.getWorld().getUID().equals(request.originWorld)) {
            return;
        }

        cancel(player.getUniqueId(), true);
    }

    public void clear() {
        for (Pending request : pending.values()) request.cancelled = true;
        pending.clear();
        pendingCount = 0;
        cooldowns.clear();
    }

    public void teleport(@NotNull Player player, @NotNull Location target) {
        if (!asyncChecked) {
            asyncChecked = true;
            try {
                teleportAsync = Player.class.getMethod("teleportAsync", Location.class);
            } catch (NoSuchMethodException exception) {
                teleportAsync = null;
            }
        }

        if (teleportAsync != null) {
            try {
                teleportAsync.invoke(player, target);
                return;
            } catch (ReflectiveOperationException ignored) {
            }
        }

        player.teleport(target);
    }
}
