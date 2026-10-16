package com.aquaticstudios.aquagraves.hologram;

import com.aquaticstudios.aquagraves.config.template.GraveField;
import com.aquaticstudios.aquagraves.config.template.GraveTemplate;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public interface Hologram {
    void spawn(@NotNull Location location, @NotNull GraveField.Source source, @NotNull GraveTemplate[] lines);

    void update(@NotNull GraveField.Source source, @NotNull GraveTemplate[] lines);

    void remove();

    boolean isSpawned();
}
