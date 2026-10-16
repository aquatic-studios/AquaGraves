package com.aquaticstudios.aquagraves.grave;

import com.aquaticstudios.aquagraves.config.GraveConfig;
import com.aquaticstudios.aquagraves.config.MessageBundle;
import com.aquaticstudios.aquagraves.platform.PlatformScheduler;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public interface GraveContext {
    @NotNull
    GraveConfig config();

    @NotNull
    MessageBundle messages();

    @NotNull
    PlatformScheduler scheduler();

    @NotNull
    GraveRegistry registry();

    @NotNull
    Logger logger();

    void contentsChanged(@NotNull Grave grave);

    void graveRemoved(@NotNull Grave grave, @NotNull RemoveReason reason);
}
