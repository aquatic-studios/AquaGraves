package com.aquaticstudios.aquagraves.config;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public final class YamlFile {
    private final Plugin plugin;
    private final String name;

    private YamlDocument document;

    public YamlFile(@NotNull Plugin plugin, @NotNull String name) {
        this.plugin = plugin;
        this.name = name;
    }

    public void load() throws IOException {
        document = YamlDocument.create(
                new File(plugin.getDataFolder(), name),
                plugin.getResource(name),
                GeneralSettings.builder().setUseDefaults(false).build(),
                LoaderSettings.DEFAULT,
                DumperSettings.DEFAULT,
                UpdaterSettings.builder().setKeepAll(true).setVersioning(new BasicVersioning("version")).build());
        document.update();
    }

    public void reload() throws IOException {
        if (document == null) {
            load();
            return;
        }
        document.reload();
    }

    @NotNull
    public YamlDocument document() {
        if (document == null) throw new IllegalStateException(name + " has not been loaded yet");
        return document;
    }

    @NotNull
    public String name() {
        return name;
    }
}
