package com.aquaticstudios.aquagraves.util;

import org.jetbrains.annotations.NotNull;

public final class TimeFormatter {
    private final String day;
    private final String hour;
    private final String minute;
    private final String second;
    private final String now;
    private final String never;

    public TimeFormatter(@NotNull String day, @NotNull String hour, @NotNull String minute,
                         @NotNull String second, @NotNull String now, @NotNull String never) {
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.now = now;
        this.never = never;
    }

    @NotNull
    public String never() {
        return never;
    }

    public void append(@NotNull StringBuilder out, long millis) {
        if (millis == Long.MAX_VALUE) {
            out.append(never);
            return;
        }
        if (millis <= 0L) {
            out.append(now);
            return;
        }

        final long totalSeconds = millis / 1000L;
        final long days = totalSeconds / 86400L;
        final long hours = (totalSeconds % 86400L) / 3600L;
        final long minutes = (totalSeconds % 3600L) / 60L;
        final long seconds = totalSeconds % 60L;

        if (days > 0L) {
            out.append(days).append(day);
            if (hours > 0L) out.append(' ').append(hours).append(hour);
            return;
        }
        if (hours > 0L) {
            out.append(hours).append(hour);
            if (minutes > 0L) out.append(' ').append(minutes).append(minute);
            return;
        }
        if (minutes > 0L) {
            out.append(minutes).append(minute);
            if (seconds > 0L) out.append(' ').append(seconds).append(second);
            return;
        }
        out.append(seconds).append(second);
    }

    @NotNull
    public String format(long millis) {
        final StringBuilder out = new StringBuilder(12);
        append(out, millis);
        return out.toString();
    }
}
