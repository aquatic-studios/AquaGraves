package com.aquaticstudios.aquagraves.api.event;

import com.aquaticstudios.aquagraves.grave.Grave;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public abstract class GraveEvent extends Event {
    private final Grave grave;

    protected GraveEvent(@NotNull Grave grave) {
        this.grave = grave;
    }

    @NotNull
    public Grave getGrave() {
        return grave;
    }
}
