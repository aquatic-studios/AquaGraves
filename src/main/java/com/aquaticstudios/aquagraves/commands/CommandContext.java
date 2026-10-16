package com.aquaticstudios.aquagraves.commands;

import com.aquaticstudios.aquagraves.config.ConfigService;
import com.aquaticstudios.aquagraves.config.MessageBundle;
import com.aquaticstudios.aquagraves.grave.GraveRegistry;
import com.aquaticstudios.aquagraves.grave.GraveService;
import com.aquaticstudios.aquagraves.grave.GraveTicker;
import com.aquaticstudios.aquagraves.player.PlayerLookup;
import com.aquaticstudios.aquagraves.player.TeleportService;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class CommandContext {
    public final Plugin plugin;
    public final ConfigService configs;
    public final GraveService graves;
    public final GraveRegistry registry;
    public final TeleportService teleports;
    public final PlayerLookup lookup;
    public final GraveTicker ticker;

    public CommandContext(@NotNull Plugin plugin,
                          @NotNull ConfigService configs,
                          @NotNull GraveService graves,
                          @NotNull TeleportService teleports,
                          @NotNull PlayerLookup lookup,
                          @NotNull GraveTicker ticker) {
        this.plugin = plugin;
        this.configs = configs;
        this.graves = graves;
        this.registry = graves.graves();
        this.teleports = teleports;
        this.lookup = lookup;
        this.ticker = ticker;
    }

    @NotNull
    public MessageBundle messages() {
        return configs.messages();
    }
}
