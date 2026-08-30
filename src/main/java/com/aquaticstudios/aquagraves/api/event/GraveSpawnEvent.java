package com.aquaticstudios.aquagraves.api.event;

import com.aquaticstudios.aquagraves.grave.Grave;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GraveSpawnEvent extends GraveEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;

    public GraveSpawnEvent(@NotNull Player player, @NotNull Grave grave) {
        super(grave);
        this.player = player;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
