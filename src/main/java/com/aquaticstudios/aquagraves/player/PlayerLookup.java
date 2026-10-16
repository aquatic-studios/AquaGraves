package com.aquaticstudios.aquagraves.player;

import com.aquaticstudios.aquagraves.grave.Grave;
import com.aquaticstudios.aquagraves.grave.GraveRegistry;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class PlayerLookup {
    private final GraveRegistry registry;
    private final String unknownName;

    public PlayerLookup(@NotNull GraveRegistry registry, @NotNull String unknownName) {
        this.registry = registry;
        this.unknownName = unknownName;
    }

    @Nullable
    public UUID resolve(@NotNull String name) {
        final Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();

        for (Grave grave : registry.all()) {
            if (grave.ownerName().equalsIgnoreCase(name)) return grave.ownerId();
        }

        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            if (name.equalsIgnoreCase(offline.getName())) return offline.getUniqueId();
        }

        return null;
    }

    @NotNull
    public String nameOf(@NotNull UUID uuid) {
        final Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();

        final List<Grave> owned = registry.ofOwner(uuid);
        if (!owned.isEmpty()) return owned.get(0).ownerName();

        final String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name == null ? unknownName : name;
    }

    @NotNull
    public List<String> suggestions(@NotNull String typed) {
        final Set<String> names = new LinkedHashSet<>();

        for (Player player : Bukkit.getOnlinePlayers()) names.add(player.getName());
        for (Grave grave : registry.all()) names.add(grave.ownerName());

        final String lower = typed.toLowerCase();
        final List<String> results = new ArrayList<>();

        for (String name : names) {
            if (!name.toLowerCase().startsWith(lower)) continue;
            results.add(name);
        }
        return results;
    }
}
