package com.aquaticstudios.aquagraves.api.event;

import com.aquaticstudios.aquagraves.grave.Grave;
import com.aquaticstudios.aquagraves.grave.RemoveReason;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GraveRemoveEvent extends GraveEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final RemoveReason reason;

    public GraveRemoveEvent(@NotNull Grave grave, @NotNull RemoveReason reason) {
        super(grave);
        this.reason = reason;
    }

    @NotNull
    public RemoveReason getReason() {
        return reason;
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
