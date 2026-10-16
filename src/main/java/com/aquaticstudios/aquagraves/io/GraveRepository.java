package com.aquaticstudios.aquagraves.io;

import com.aquaticstudios.aquagraves.config.ConfigService;
import com.aquaticstudios.aquagraves.grave.Grave;
import com.aquaticstudios.aquagraves.grave.GraveRegistry;
import com.aquaticstudios.aquagraves.platform.PlatformScheduler;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class GraveRepository {
    private final IoWorker io;
    private final DataFile file;
    private final GraveRegistry registry;
    private final PlatformScheduler scheduler;
    private final ConfigService configs;
    private final Logger logger;

    private final AtomicBoolean dirty = new AtomicBoolean();
    private final AtomicBoolean flushScheduled = new AtomicBoolean();

    private volatile int lastWrittenSize = 256;

    public GraveRepository(@NotNull Path path,
                           @NotNull IoWorker io,
                           @NotNull GraveRegistry registry,
                           @NotNull PlatformScheduler scheduler,
                           @NotNull ConfigService configs,
                           @NotNull Logger logger) {
        this.file = new DataFile(path);
        this.io = io;
        this.registry = registry;
        this.scheduler = scheduler;
        this.configs = configs;
        this.logger = logger;
    }

    @NotNull
    public CompletableFuture<List<GraveRecord>> readAsync() {
        return io.supply(() -> {
            try {
                final List<String> lines = file.readLines();
                if (lines.isEmpty()) return Collections.<GraveRecord>emptyList();

                final List<GraveRecord> records = new ArrayList<>(lines.size());
                int malformed = 0;

                for (int i = 0; i < lines.size(); i++) {
                    final GraveRecord record = GraveCodec.decode(lines.get(i));
                    if (record == null) malformed++;
                    else records.add(record);
                }

                if (malformed > 0) {
                    logger.warning("Skipped " + malformed + " malformed grave record(s) in " + file.path().getFileName());
                }
                return records;
            } catch (Exception exception) {
                logger.log(Level.SEVERE, "Could not read the grave data file", exception);
                return Collections.<GraveRecord>emptyList();
            }
        });
    }

    public void markDirty() {
        dirty.set(true);
        if (!configs.config().saveOnChange) return;

        if (!flushScheduled.compareAndSet(false, true)) return;

        scheduler.globalLater(() -> {
            flushScheduled.set(false);
            flush();
        }, configs.config().saveCoalesceTicks);
    }

    public void flushIfDirty() {
        if (!dirty.get()) return;
        flush();
    }

    public void flush() {
        if (!dirty.compareAndSet(true, false)) return;

        final StringBuilder out = new StringBuilder(lastWrittenSize);
        final boolean deferred = build(out);

        lastWrittenSize = Math.max(256, out.length() + 64);
        io.run(() -> write(out));

        if (deferred) markDirty();
    }

    public void flushBlocking() {
        final StringBuilder out = new StringBuilder(lastWrittenSize);
        build(out);
        dirty.set(false);
        write(out);
    }

    private boolean build(StringBuilder out) {
        out.append(GraveCodec.HEADER).append('\n');

        boolean deferred = false;

        for (Grave grave : registry.all()) {
            if (grave.isRemoved()) continue;

            String line = grave.encodedLine();

            if (grave.isEncodingStale()) {
                if (scheduler.ownsRegion(grave.locationRef())) {
                    line = grave.refreshEncoding();
                } else {
                    scheduler.execute(grave.locationRef(), grave::refreshEncoding);
                    deferred = true;
                }
            }

            if (line == null) {
                deferred = true;
                continue;
            }

            out.append(line).append('\n');
        }

        return deferred;
    }

    private void write(CharSequence content) {
        try {
            file.write(content);
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Could not write the grave data file", exception);
        }
    }
}
