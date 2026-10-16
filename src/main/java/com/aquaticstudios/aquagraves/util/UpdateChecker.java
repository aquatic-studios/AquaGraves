package com.aquaticstudios.aquagraves.util;

import com.aquaticstudios.aquagraves.config.ConfigService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public final class UpdateChecker {
    private static final String PERMISSION = "update";

    private static final int RESOURCE_ID = 0;
    private static final String API = "https://api.spigotmc.org/legacy/update.php?resource=" + RESOURCE_ID;
    private static final String PAGE = "https://www.spigotmc.org/resources/" + RESOURCE_ID + "/";

    private final Plugin plugin;
    private final ConfigService configs;

    private volatile String latest;

    public UpdateChecker(@NotNull Plugin plugin, @NotNull ConfigService configs) {
        this.plugin = plugin;
        this.configs = configs;
    }

    public void check() {
        if (RESOURCE_ID <= 0) return;
        if (!configs.config().updateNotify) return;

        CompletableFuture.supplyAsync(UpdateChecker::fetch).thenAccept(version -> {
            if (version == null || version.isEmpty() || version.equalsIgnoreCase(current())) return;

            this.latest = version;
            send(Bukkit.getConsoleSender(), "&#33CCFF[" + name() + "] &fA newer version is available: &#33CCFF"
                    + version + " &f(you are on &#FF3F3F" + current() + "&f)");
        });
    }

    public void announce(@NotNull Player player) {
        final String version = latest;
        if (version == null) return;
        if (!configs.config().updateNotify) return;
        if (!Permissions.has(player, PERMISSION)) return;

        send(player, "");
        send(player, "  &#33CCFF&l" + name() + " &fA new version is available!");
        send(player, "  &fYour version: &#FF3F3F" + current() + " &f| Latest: &#33CCFF" + version);
        send(player, "");
        send(player, "  &fDownload it here:");
        send(player, "  &7" + PAGE);
        send(player, "");
    }

    private void send(@NotNull CommandSender target, @NotNull String message) {
        target.sendMessage(CC.legacy(message));
    }

    private String name() {
        return plugin.getDescription().getName();
    }

    private String current() {
        return plugin.getDescription().getVersion();
    }

    private static String fetch() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(API).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "AquaGraves-UpdateChecker");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return null;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                final String line = reader.readLine();
                return line == null ? null : line.trim();
            }
        } catch (Exception exception) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
