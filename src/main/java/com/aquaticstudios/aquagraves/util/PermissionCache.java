package com.aquaticstudios.aquagraves.util;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PermissionCache {
    private static final long TTL_MILLIS = 30_000L;
    private static final int ABSENT = Integer.MIN_VALUE;
    private static final String PREFIX = Permissions.ROOT + ".";

    private static final class Entry {
        private final long expiresAt;
        private final int limit;
        private final int protectionSeconds;

        private Entry(long expiresAt, int limit, int protectionSeconds) {
            this.expiresAt = expiresAt;
            this.limit = limit;
            this.protectionSeconds = protectionSeconds;
        }
    }

    private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();

    public int limit(@NotNull Player player, int fallback) {
        final int value = resolve(player).limit;
        return value == ABSENT ? fallback : value;
    }

    public int protectionSeconds(@NotNull Player player, int fallback) {
        final int value = resolve(player).protectionSeconds;
        return value == ABSENT ? fallback : value;
    }

    public void invalidate(@NotNull UUID uuid) {
        entries.remove(uuid);
    }

    public void clear() {
        entries.clear();
    }

    public int size() {
        return entries.size();
    }

    private Entry resolve(Player player) {
        final UUID uuid = player.getUniqueId();
        final long now = System.currentTimeMillis();

        final Entry cached = entries.get(uuid);
        if (cached != null && cached.expiresAt > now) return cached;

        int limit = ABSENT;
        int protection = ABSENT;

        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue()) continue;

            final String node = info.getPermission();
            if (node.isEmpty() || node.charAt(0) != 'a') continue;
            if (!node.startsWith(PREFIX)) continue;

            final String tail = node.substring(PREFIX.length());
            if (tail.startsWith("limit.")) limit = merge(limit, tail.substring(6));
            else if (tail.startsWith("protection.")) protection = merge(protection, tail.substring(11));
        }

        final Entry entry = new Entry(now + TTL_MILLIS, limit, protection);
        entries.put(uuid, entry);
        return entry;
    }

    private static int merge(int current, String value) {
        if (current == -1) return -1;
        if (value.equals("*") || value.equalsIgnoreCase("unlimited")) return -1;

        try {
            final int parsed = Integer.parseInt(value);
            return current == ABSENT ? parsed : Math.max(current, parsed);
        } catch (NumberFormatException exception) {
            return current;
        }
    }
}
