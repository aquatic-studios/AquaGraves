package com.aquaticstudios.aquagraves.config;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public final class ConfigService {
    private final Plugin plugin;
    private final YamlFile configFile;
    private final YamlFile messagesFile;

    private volatile GraveConfig config;
    private volatile MessageBundle messages;

    public ConfigService(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.configFile = new YamlFile(plugin, "config.yml");
        this.messagesFile = new YamlFile(plugin, "messages.yml");
    }

    public void load() throws IOException {
        configFile.load();
        messagesFile.load();
        snapshot();
    }

    public boolean reload() {
        try {
            configFile.reload();
            messagesFile.reload();
            snapshot();
            return true;
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not reload configuration, keeping the previous one: " + exception.getMessage());
            return false;
        }
    }

    @NotNull
    public GraveConfig config() {
        return config;
    }

    @NotNull
    public MessageBundle messages() {
        return messages;
    }

    private void snapshot() {
        final GraveConfig parsedConfig = GraveConfig.from(configFile.document(), plugin.getLogger());
        final MessageBundle parsedMessages = MessageBundle.from(messagesFile.document());

        this.config = parsedConfig;
        this.messages = parsedMessages;
    }
}
