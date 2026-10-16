package com.aquaticstudios.aquagraves.io;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DataFile {
    private final Path path;
    private final Path temporary;

    public DataFile(@NotNull Path path) {
        this.path = path;
        this.temporary = path.resolveSibling(path.getFileName() + ".tmp");
    }

    @NotNull
    public Path path() {
        return path;
    }

    public boolean exists() {
        return Files.exists(path);
    }

    @NotNull
    public List<String> readLines() throws IOException {
        if (!Files.exists(path)) return Collections.emptyList();

        final List<String> lines = new ArrayList<>(256);
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') continue;
                lines.add(line);
            }
        }
        return lines;
    }

    public void write(@NotNull CharSequence content) throws IOException {
        final Path parent = path.getParent();
        if (parent != null) Files.createDirectories(parent);

        Files.write(temporary, content.toString().getBytes(StandardCharsets.UTF_8));

        try {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicUnsupported) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
