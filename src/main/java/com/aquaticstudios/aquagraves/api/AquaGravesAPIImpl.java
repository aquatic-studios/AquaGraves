package com.aquaticstudios.aquagraves.api;

import com.aquaticstudios.aquagraves.grave.Grave;
import com.aquaticstudios.aquagraves.grave.GraveRegistry;
import com.aquaticstudios.aquagraves.grave.GraveService;
import com.aquaticstudios.aquagraves.grave.RemoveReason;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class AquaGravesAPIImpl implements AquaGravesAPI {
    private final GraveService graves;
    private final GraveRegistry registry;

    public AquaGravesAPIImpl(@NotNull GraveService graves) {
        this.graves = graves;
        this.registry = graves.graves();
    }

    @NotNull
    @Override
    public Collection<Grave> graves() {
        return Collections.unmodifiableCollection(registry.all());
    }

    @NotNull
    @Override
    public List<Grave> gravesOf(@NotNull UUID ownerId) {
        return new ArrayList<>(registry.ofOwner(ownerId));
    }

    @NotNull
    @Override
    public List<Grave> gravesOf(@NotNull OfflinePlayer owner) {
        return gravesOf(owner.getUniqueId());
    }

    @Override
    public int graveCount() {
        return registry.size();
    }

    @Nullable
    @Override
    public Grave grave(@NotNull UUID id) {
        return registry.byId(id);
    }

    @Nullable
    @Override
    public Grave graveAt(@NotNull Location location) {
        return registry.at(location);
    }

    @NotNull
    @Override
    public List<Grave> gravesNear(@NotNull Location center, double radius) {
        return registry.near(center, radius);
    }

    @Override
    public int graveLimit(@NotNull Player player) {
        return graves.limitFor(player);
    }

    @Override
    public int protectionSeconds(@NotNull Player player) {
        return graves.protectionSecondsFor(player);
    }

    @NotNull
    @Override
    public Grave createGrave(@NotNull Location location,
                             @NotNull OfflinePlayer owner,
                             @NotNull List<ItemStack> items,
                             int storedXp,
                             long protectionSeconds) {
        final long protectedUntil = protectionSeconds <= 0
                ? Long.MAX_VALUE
                : System.currentTimeMillis() + protectionSeconds * 1000L;

        return graves.create(location, owner.getUniqueId(), owner.getName(), items, storedXp, protectedUntil);
    }

    @Override
    public void removeGrave(@NotNull Grave grave) {
        grave.remove(RemoveReason.PLUGIN);
    }
}
