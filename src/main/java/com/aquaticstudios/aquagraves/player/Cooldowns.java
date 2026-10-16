package com.aquaticstudios.aquagraves.player;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Cooldowns {
    private final Map<UUID, Long> until = new ConcurrentHashMap<>();

    public long remaining(@NotNull UUID uuid) {
        if (until.isEmpty()) return 0L;

        final Long deadline = until.get(uuid);
        if (deadline == null) return 0L;

        final long left = deadline - System.currentTimeMillis();
        if (left <= 0L) {
            until.remove(uuid, deadline);
            return 0L;
        }
        return left;
    }

    public void apply(@NotNull UUID uuid, long millis) {
        if (millis <= 0L) return;
        until.put(uuid, System.currentTimeMillis() + millis);
    }

    public void clear(@NotNull UUID uuid) {
        until.remove(uuid);
    }

    public void clear() {
        until.clear();
    }
}
