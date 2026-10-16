package com.aquaticstudios.aquagraves.listeners;

import com.aquaticstudios.aquagraves.grave.Grave;
import com.aquaticstudios.aquagraves.grave.GraveEntities;
import com.aquaticstudios.aquagraves.grave.GraveRegistry;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GraveInteractListener implements Listener {
    private final GraveRegistry registry;

    public GraveInteractListener(@NotNull GraveRegistry registry) {
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (registry.isEmpty()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() == null) return;

        final Grave grave = resolve(event.getClickedBlock());
        if (grave == null) return;

        event.setCancelled(true);
        grave.interact(event.getPlayer(), event.getHand() == EquipmentSlot.HAND);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractAtEntity(@NotNull PlayerInteractAtEntityEvent event) {
        final Entity entity = event.getRightClicked();
        if (!GraveEntities.couldBeGraveEntity(entity.getType())) return;

        final Grave grave = registry.byEntity(entity.getUniqueId());
        if (grave == null) return;

        event.setCancelled(true);
        grave.interact(event.getPlayer(), event.getHand() == EquipmentSlot.HAND);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onArmorStandManipulate(@NotNull PlayerArmorStandManipulateEvent event) {
        final Grave grave = registry.byEntity(event.getRightClicked().getUniqueId());
        if (grave == null) return;

        event.setCancelled(true);
        grave.interact(event.getPlayer(), event.getHand() == EquipmentSlot.HAND);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(@NotNull EntityDamageEvent event) {
        final Entity entity = event.getEntity();
        if (!GraveEntities.couldBeGraveEntity(entity.getType())) return;
        if (registry.byEntity(entity.getUniqueId()) == null) return;

        event.setCancelled(true);
    }

    @Nullable
    private Grave resolve(@Nullable Block block) {
        if (block == null) return null;

        final World world = block.getWorld();
        final int x = block.getX();
        final int y = block.getY();
        final int z = block.getZ();

        Grave grave = registry.at(world, x, y, z);
        if (grave != null) return grave;

        grave = registry.at(world, x, y + 1, z);
        if (grave != null) return grave;

        return registry.at(world, x, y - 1, z);
    }
}
