package com.aquaticstudios.aquagraves.api.event;

import com.aquaticstudios.aquagraves.grave.Grave;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GraveUnlockEvent extends GraveEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    public GraveUnlockEvent(@NotNull Grave grave) {
        super(grave);
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
