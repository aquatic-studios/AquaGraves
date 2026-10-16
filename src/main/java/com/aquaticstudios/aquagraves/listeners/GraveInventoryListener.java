package com.aquaticstudios.aquagraves.listeners;

import com.aquaticstudios.aquagraves.config.ConfigService;
import com.aquaticstudios.aquagraves.grave.Grave;
import com.aquaticstudios.aquagraves.grave.GraveHolder;
import com.aquaticstudios.aquagraves.grave.GraveTick;
import com.aquaticstudios.aquagraves.platform.PlatformScheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GraveInventoryListener implements Listener {
    private final Plugin plugin;
    private final ConfigService configs;
    private final PlatformScheduler scheduler;

    public GraveInventoryListener(@NotNull Plugin plugin, @NotNull ConfigService configs,
                                  @NotNull PlatformScheduler scheduler) {
        this.plugin = plugin;
        this.configs = configs;
        this.scheduler = scheduler;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onClick(@NotNull InventoryClickEvent event) {
        final Grave grave = graveOf(event.getView().getTopInventory());
        if (grave == null) return;

        if (!(event.getWhoClicked() instanceof Player)) {
            event.setCancelled(true);
            return;
        }

        final Player player = (Player) event.getWhoClicked();

        if (grave.isRemoved()) {
            event.setCancelled(true);
            close(player);
            configs.messages().send(player, "messages.grave-gone");
            return;
        }

        if (!grave.canAccess(player)) {
            event.setCancelled(true);
            close(player);
            configs.messages().sendGrave(player, "messages.grave-owner-only", grave);
            return;
        }

        if (!configs.config().allowDeposits && isDeposit(event)) {
            event.setCancelled(true);
            configs.messages().send(player, "messages.grave-deposit-denied");
            return;
        }

        grave.markChanged();
        refreshNextTick(grave);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrag(@NotNull InventoryDragEvent event) {
        final Inventory top = event.getView().getTopInventory();
        final Grave grave = graveOf(top);
        if (grave == null) return;

        if (!(event.getWhoClicked() instanceof Player)) {
            event.setCancelled(true);
            return;
        }

        final Player player = (Player) event.getWhoClicked();

        if (grave.isRemoved() || !grave.canAccess(player)) {
            event.setCancelled(true);
            close(player);
            return;
        }

        if (!configs.config().allowDeposits) {
            final int topSize = top.getSize();
            for (int slot : event.getRawSlots()) {
                if (slot >= topSize) continue;

                event.setCancelled(true);
                configs.messages().send(player, "messages.grave-deposit-denied");
                return;
            }
        }

        grave.markChanged();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(@NotNull InventoryCloseEvent event) {
        final Grave grave = graveOf(event.getView().getTopInventory());
        if (grave == null) return;

        grave.markChanged();
        refreshNextTick(grave);
    }

    private void refreshNextTick(Grave grave) {
        if (!plugin.isEnabled()) return;

        scheduler.executeLater(grave.locationRef(), () -> {
            if (grave.isRemoved()) return;
            grave.tick(GraveTick.immediate(configs.config(), configs.messages()));
        }, 1L);
    }

    private boolean isDeposit(@NotNull InventoryClickEvent event) {
        final int topSize = event.getView().getTopInventory().getSize();
        final InventoryAction action = event.getAction();

        if (event.getRawSlot() >= 0 && event.getRawSlot() < topSize) {
            switch (action) {
                case PLACE_ONE:
                case PLACE_SOME:
                case PLACE_ALL:
                case SWAP_WITH_CURSOR:
                case HOTBAR_SWAP:
                case HOTBAR_MOVE_AND_READD:
                    return true;
                default:
                    return false;
            }
        }

        return action == InventoryAction.MOVE_TO_OTHER_INVENTORY;
    }

    private void close(@NotNull Player player) {
        if (!plugin.isEnabled()) return;
        scheduler.executeLater(player.getLocation(), player::closeInventory, 1L);
    }

    @Nullable
    private Grave graveOf(@Nullable Inventory inventory) {
        if (inventory == null) return null;

        final InventoryHolder holder = inventory.getHolder();
        if (!(holder instanceof GraveHolder)) return null;

        return ((GraveHolder) holder).getGrave();
    }
}
