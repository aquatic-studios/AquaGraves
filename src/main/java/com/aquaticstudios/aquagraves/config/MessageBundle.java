package com.aquaticstudios.aquagraves.config;

import com.aquaticstudios.aquagraves.config.template.GraveField;
import com.aquaticstudios.aquagraves.config.template.GraveTemplate;
import com.aquaticstudios.aquagraves.util.CC;
import com.aquaticstudios.aquagraves.util.TimeFormatter;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MessageBundle {
    private final Map<String, String> strings;
    private final Map<String, List<String>> lists;
    private final Map<String, GraveTemplate> templates;
    private final Map<String, GraveTemplate[]> listTemplates;

    public final GraveTemplate[] hologramLines;
    public final GraveTemplate hologramLocked;
    public final String hologramUnlocked;
    public final String hologramForever;

    public final String prefix;
    private final String rawPrefix;
    public final String guiTitle;
    public final String unknownPlayer;
    public final TimeFormatter time;

    private MessageBundle(@NotNull YamlDocument yaml) {
        final String rawPrefix = yaml.getString("prefix", "");
        this.prefix = CC.legacy(rawPrefix);
        this.rawPrefix = rawPrefix;

        final Map<String, String> rawStrings = new HashMap<>();
        final Map<String, List<String>> rawLists = new HashMap<>();
        flatten(yaml, "", rawStrings, rawLists);

        final Map<String, String> flatStrings = new HashMap<>(rawStrings.size() * 2);
        final Map<String, GraveTemplate> compiled = new HashMap<>(rawStrings.size() * 2);
        for (Map.Entry<String, String> entry : rawStrings.entrySet()) {
            flatStrings.put(entry.getKey(), CC.legacy(entry.getValue()));
            compiled.put(entry.getKey(), GraveTemplate.compile(entry.getValue()));
        }

        final Map<String, List<String>> flatLists = new HashMap<>(rawLists.size() * 2);
        final Map<String, GraveTemplate[]> compiledLists = new HashMap<>(rawLists.size() * 2);
        for (Map.Entry<String, List<String>> entry : rawLists.entrySet()) {
            final List<String> raw = entry.getValue();
            final List<String> formatted = new ArrayList<>(raw.size());
            for (int i = 0; i < raw.size(); i++) formatted.add(CC.legacy(raw.get(i)));

            flatLists.put(entry.getKey(), Collections.unmodifiableList(formatted));
            compiledLists.put(entry.getKey(), GraveTemplate.compileAll(raw));
        }

        this.strings = Collections.unmodifiableMap(flatStrings);
        this.lists = Collections.unmodifiableMap(flatLists);
        this.templates = Collections.unmodifiableMap(compiled);
        this.listTemplates = Collections.unmodifiableMap(compiledLists);

        this.hologramLines = GraveTemplate.compileAll(rawList(yaml, "hologram"));
        this.hologramLocked = GraveTemplate.compile(yaml.getString("hologram-locked", ""), true);
        this.hologramUnlocked = CC.legacy(yaml.getString("hologram-unlocked", ""));
        this.hologramForever = CC.legacy(yaml.getString("hologram-forever", ""));

        this.guiTitle = CC.legacy(yaml.getString("gui-title", "&0%player%"));
        this.unknownPlayer = CC.strip(yaml.getString("unknown-player", "Unknown"));

        this.time = new TimeFormatter(
                yaml.getString("time.day", "d"),
                yaml.getString("time.hour", "h"),
                yaml.getString("time.minute", "m"),
                yaml.getString("time.second", "s"),
                yaml.getString("time.now", "now"),
                yaml.getString("time.never", "never"));
    }

    @NotNull
    public static MessageBundle from(@NotNull YamlDocument yaml) {
        return new MessageBundle(yaml);
    }

    @NotNull
    public String string(@NotNull String path) {
        final String value = strings.get(path);
        return value == null ? "" : value;
    }

    @NotNull
    public List<String> list(@NotNull String path) {
        final List<String> value = lists.get(path);
        return value == null ? Collections.emptyList() : value;
    }

    @Nullable
    public GraveTemplate template(@NotNull String path) {
        return templates.get(path);
    }

    public void send(@Nullable CommandSender to, @NotNull String path) {
        if (to == null) return;

        final String message = string(path);
        if (message.isEmpty()) return;

        to.sendMessage(message);
    }

    public void send(@Nullable CommandSender to, @NotNull String path, @NotNull String... pairs) {
        if (to == null) return;

        final String message = string(path);
        if (message.isEmpty()) return;

        to.sendMessage(replace(message, pairs));
    }

    public void sendGrave(@Nullable CommandSender to, @NotNull String path, @NotNull GraveField.Source grave) {
        if (to == null) return;

        final GraveTemplate template = templates.get(path);
        if (template == null) return;

        final String message = template.render(grave);
        if (message.isEmpty()) return;

        to.sendMessage(message);
    }

    public void sendList(@Nullable CommandSender to, @NotNull String path, @NotNull String... pairs) {
        if (to == null) return;

        final List<String> lines = list(path);
        for (int i = 0; i < lines.size(); i++) {
            to.sendMessage(replace(lines.get(i), pairs));
        }
    }

    public void sendGraveList(@Nullable CommandSender to, @NotNull String path, @NotNull GraveField.Source grave) {
        if (to == null) return;

        final GraveTemplate[] compiled = listTemplates.get(path);
        if (compiled == null) return;

        for (GraveTemplate line : compiled) {
            to.sendMessage(line.render(grave));
        }
    }

    @NotNull
    public String replace(@NotNull String message, @NotNull String... pairs) {
        String out = message;
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            out = out.replace(pairs[i], pairs[i + 1]);
        }
        return out;
    }

    private void flatten(Section section, String prefixPath,
                         Map<String, String> outStrings, Map<String, List<String>> outLists) {
        for (Object routeKey : section.getRoutesAsStrings(false)) {
            final String key = String.valueOf(routeKey);
            final String path = prefixPath.isEmpty() ? key : prefixPath + "." + key;
            final Object value = section.get(key);

            if (value instanceof Section) {
                flatten((Section) value, path, outStrings, outLists);
                continue;
            }

            if (value instanceof List) {
                final List<?> raw = (List<?>) value;
                final List<String> lines = new ArrayList<>(raw.size());
                for (Object line : raw) {
                    lines.add(withPrefix(String.valueOf(line)));
                }
                outLists.put(path, lines);
                continue;
            }

            if (value == null) continue;
            outStrings.put(path, withPrefix(String.valueOf(value)));
        }
    }

    private List<String> rawList(YamlDocument yaml, String path) {
        final List<String> raw = yaml.getStringList(path);
        return raw == null ? Collections.emptyList() : raw;
    }

    private String withPrefix(String raw) {
        return raw.indexOf('%') < 0 ? raw : raw.replace("%prefix%", rawPrefix);
    }
}
