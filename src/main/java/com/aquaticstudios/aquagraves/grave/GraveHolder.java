package com.aquaticstudios.aquagraves.grave;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GraveHolder implements InventoryHolder {
    private Grave grave;
    private Inventory inventory;

    void bind(@NotNull Grave grave, @NotNull Inventory inventory) {
        this.grave = grave;
        this.inventory = inventory;
    }

    @Nullable
    public Grave getGrave() {
        return grave;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
