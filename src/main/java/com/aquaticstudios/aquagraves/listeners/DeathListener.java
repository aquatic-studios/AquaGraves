package com.aquaticstudios.aquagraves.listeners;

import com.aquaticstudios.aquagraves.api.event.GravePreSpawnEvent;
import com.aquaticstudios.aquagraves.api.event.GraveSpawnEvent;
import com.aquaticstudios.aquagraves.config.ConfigService;
import com.aquaticstudios.aquagraves.config.GraveConfig;
import com.aquaticstudios.aquagraves.grave.Grave;
import com.aquaticstudios.aquagraves.grave.GraveService;
import com.aquaticstudios.aquagraves.grave.LimitAction;
import com.aquaticstudios.aquagraves.util.ExperienceUtil;
import com.aquaticstudios.aquagraves.util.InventoryUtil;
import com.aquaticstudios.aquagraves.util.Permissions;
import com.aquaticstudios.aquagraves.util.Sounds;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.EventExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public final class DeathListener implements Listener {
    private final Plugin plugin;
    private final ConfigService configs;
    private final GraveService graves;
    private final Logger logger;

    public DeathListener(@NotNull Plugin plugin, @NotNull ConfigService configs,
                         @NotNull GraveService graves, @NotNull Logger logger) {
        this.plugin = plugin;
        this.configs = configs;
        this.graves = graves;
        this.logger = logger;
    }

    public void register() {
        final EventExecutor executor = (listener, event) -> {
            if (!(event instanceof PlayerDeathEvent)) return;
            onDeath((PlayerDeathEvent) event);
        };

        plugin.getServer().getPluginManager().registerEvent(
                PlayerDeathEvent.class,
                this,
                configs.config().deathPriority,
                executor,
                plugin,
                true);
    }

    private void onDeath(@NotNull PlayerDeathEvent event) {
        final Player player = event.getEntity();
        final GraveConfig config = configs.config();

        if (!config.disabledWorlds.isEmpty()
                && config.disabledWorlds.contains(player.getWorld().getName())) {
            if (config.debug) logger.info("[debug] " + player.getName() + ": world is disabled");
            return;
        }

        if (!Permissions.canHaveGraves(player)) {
            if (config.debug) logger.info("[debug] " + player.getName() + ": missing aquagraves.use");
            return;
        }

        if (!config.deniedDeathCauses.isEmpty()
                && player.getLastDamageCause() != null
                && config.deniedDeathCauses.contains(player.getLastDamageCause().getCause().name())) {
            if (config.debug) logger.info("[debug] " + player.getName() + ": denied death cause");
            return;
        }

        final Location location = player.getLocation().clone().add(0.0D, -0.5D, 0.0D);

        final GravePreSpawnEvent preSpawn = new GravePreSpawnEvent(player, location);
        Bukkit.getPluginManager().callEvent(preSpawn);
        if (preSpawn.isCancelled()) {
            if (config.debug) logger.info("[debug] " + player.getName() + ": GravePreSpawnEvent cancelled");
            return;
        }

        final List<ItemStack> items = takeItems(event, player, config);
        final int xp = takeExperience(event, player, config);

        if (items.isEmpty() && xp <= 0) {
            if (config.debug) logger.info("[debug] " + player.getName() + ": nothing to store");
            return;
        }

        if (!hasRoom(player, config)) {
            event.getDrops().addAll(items);
            if (xp > 0) event.setDroppedExp(xp);

            configs.messages().send(player, "messages.grave-limit-denied",
                    "%limit%", Integer.toString(graves.limitFor(player)));
            return;
        }

        final Grave grave = graves.create(
                preSpawn.getLocation(),
                player.getUniqueId(),
                player.getName(),
                InventoryUtil.reorder(player.getInventory(), items, config),
                xp,
                graves.protectionDeadlineFor(player));

        Bukkit.getPluginManager().callEvent(new GraveSpawnEvent(player, grave));
        Sounds.playAt(grave.locationRef(), config.soundSpawn, config);

        if (config.sendCoordinates) {
            configs.messages().sendGrave(player, "messages.grave-coordinates", grave);
        }

        if (config.debug) {
            logger.info("[debug] " + player.getName() + ": grave created with "
                    + grave.itemCount() + " stack(s) and " + xp + " xp");
        }
    }

    @NotNull
    private List<ItemStack> takeItems(PlayerDeathEvent event, Player player, GraveConfig config) {
        if (!config.storeItems) return new ArrayList<>(0);

        if (!event.getKeepInventory()) {
            final List<ItemStack> items = new ArrayList<>(event.getDrops());
            event.getDrops().clear();
            return items;
        }

        if (!config.overrideKeepInventory) return new ArrayList<>(0);

        final List<ItemStack> items = new ArrayList<>(Arrays.asList(player.getInventory().getContents()));
        player.getInventory().clear();
        event.getDrops().clear();
        return items;
    }

    private int takeExperience(PlayerDeathEvent event, Player player, GraveConfig config) {
        if (!config.storeXp) return 0;

        if (event.getKeepLevel()) {
            if (!config.overrideKeepLevel) return 0;

            final int stored = Math.round(ExperienceUtil.getExp(player) * config.xpKeepPercentage);
            player.setLevel(0);
            player.setExp(0.0f);
            player.setTotalExperience(0);
            event.setDroppedExp(0);
            return stored;
        }

        final int stored = Math.round(ExperienceUtil.getExp(player) * config.xpKeepPercentage);
        event.setDroppedExp(0);
        return stored;
    }

    private boolean hasRoom(Player player, GraveConfig config) {
        final int limit = graves.limitFor(player);
        if (limit < 0) return true;

        final int owned = graves.graves().countOfOwner(player.getUniqueId());
        if (owned < limit) return true;

        if (config.limitReached == LimitAction.DENY) return false;

        configs.messages().send(player, "messages.grave-limit-reached", "%limit%", Integer.toString(limit));
        return graves.makeRoom(player);
    }
}
