package com.aquaticstudios.aquagraves.hook;

import com.aquaticstudios.aquagraves.config.ConfigService;
import com.aquaticstudios.aquagraves.grave.Grave;
import com.aquaticstudios.aquagraves.grave.GraveRegistry;
import com.aquaticstudios.aquagraves.grave.GraveService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class PlaceholderHook extends PlaceholderExpansion {
    private final Plugin plugin;
    private final ConfigService configs;
    private final GraveService graves;
    private final GraveRegistry registry;

    public PlaceholderHook(@NotNull Plugin plugin, @NotNull ConfigService configs, @NotNull GraveService graves) {
        this.plugin = plugin;
        this.configs = configs;
        this.graves = graves;
        this.registry = graves.graves();
    }

    @NotNull
    @Override
    public String getIdentifier() {
        return "aquagraves";
    }

    @NotNull
    @Override
    public String getAuthor() {
        return "Senkex";
    }

    @NotNull
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Nullable
    @Override
    public String onRequest(@Nullable OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("total")) return Integer.toString(registry.size());
        if (params.equalsIgnoreCase("owners")) return Integer.toString(registry.ownerCount());
        if (player == null) return "";

        final List<Grave> owned = registry.ofOwner(player.getUniqueId());

        switch (params.toLowerCase()) {
            case "graves":
                return Integer.toString(owned.size());
            case "limit": {
                final Player online = player.getPlayer();
                final int limit = online == null
                        ? configs.config().graveLimit
                        : graves.limitFor(online);
                return limit < 0 ? "∞" : Integer.toString(limit);
            }
            case "protected": {
                final long now = System.currentTimeMillis();
                int locked = 0;
                for (int i = 0; i < owned.size(); i++) {
                    if (owned.get(i).isProtected(now)) locked++;
                }
                return Integer.toString(locked);
            }
            default:
                break;
        }

        if (owned.isEmpty()) return "";
        final Grave last = owned.get(0);

        switch (params.toLowerCase()) {
            case "last_world":
                return last.world() == null ? "" : configs.config().worldName(last.world().getName());
            case "last_x":
                return Integer.toString(last.blockX());
            case "last_y":
                return Integer.toString(last.blockY());
            case "last_z":
                return Integer.toString(last.blockZ());
            case "last_items":
                return Integer.toString(last.itemCount());
            case "last_xp":
                return Integer.toString(last.storedXp());
            case "last_despawn":
                return configs.messages().time.format(last.remainingMillis());
            case "last_protection":
                return configs.messages().time.format(last.protectionMillis(System.currentTimeMillis()));
            case "last_locked":
                return Boolean.toString(last.isProtected());
            default:
                return null;
        }
    }
}
