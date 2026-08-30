package com.aquaticstudios.aquagraves.api;

import com.aquaticstudios.aquagraves.grave.Grave;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AquaGravesAPI {
    @Nullable
    static AquaGravesAPI get() {
        return Bukkit.getServicesManager().load(AquaGravesAPI.class);
    }

    @NotNull
    Collection<Grave> graves();

    @NotNull
    List<Grave> gravesOf(@NotNull UUID ownerId);

    @NotNull
    List<Grave> gravesOf(@NotNull OfflinePlayer owner);

    int graveCount();

    @Nullable
    Grave grave(@NotNull UUID id);

    @Nullable
    Grave graveAt(@NotNull Location location);

    @NotNull
    List<Grave> gravesNear(@NotNull Location center, double radius);

    int graveLimit(@NotNull Player player);

    int protectionSeconds(@NotNull Player player);

    @NotNull
    Grave createGrave(@NotNull Location location,
                      @NotNull OfflinePlayer owner,
                      @NotNull List<ItemStack> items,
                      int storedXp,
                      long protectionSeconds);

    void removeGrave(@NotNull Grave grave);
}
