package com.aquaticstudios.aquagraves.listeners;

import com.aquaticstudios.aquagraves.config.ConfigService;
import com.aquaticstudios.aquagraves.player.TeleportService;
import com.aquaticstudios.aquagraves.util.PermissionCache;
import com.aquaticstudios.aquagraves.util.UpdateChecker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public final class PlayerSessionListener implements Listener {
    private final ConfigService configs;
    private final TeleportService teleports;
    private final PermissionCache permissions;
    private final UpdateChecker updates;

    public PlayerSessionListener(@NotNull ConfigService configs,
                                 @NotNull TeleportService teleports,
                                 @NotNull PermissionCache permissions,
                                 @NotNull UpdateChecker updates) {
        this.configs = configs;
        this.teleports = teleports;
        this.permissions = permissions;
        this.updates = updates;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        updates.announce(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        final Player player = event.getPlayer();

        teleports.cancel(player.getUniqueId(), false);
        teleports.cooldowns().clear(player.getUniqueId());
        permissions.invalidate(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(@NotNull PlayerMoveEvent event) {
        if (!teleports.hasPending()) return;
        if (!configs.config().teleportCancelOnMove) return;
        if (event.getTo() == null) return;

        teleports.onMove(event.getPlayer(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(@NotNull EntityDamageEvent event) {
        if (!teleports.hasPending()) return;
        if (!configs.config().teleportCancelOnDamage) return;
        if (!(event.getEntity() instanceof Player)) return;

        final Player player = (Player) event.getEntity();
        if (!teleports.isPending(player.getUniqueId())) return;

        teleports.cancel(player.getUniqueId(), true);
    }
}
