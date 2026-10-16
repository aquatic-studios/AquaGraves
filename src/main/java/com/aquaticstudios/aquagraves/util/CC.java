package com.aquaticstudios.aquagraves.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CC {
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = buildSerializer();

    private static final Pattern HEX = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern BUNGEE_HEX = Pattern.compile("&x(&[0-9a-fA-F]){6}", Pattern.CASE_INSENSITIVE);
    private static final Pattern STRIP = Pattern.compile("(?i)§[0-9A-FK-ORX]");

    private static final String[] NAMED = {
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
            "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
            "yellow", "white"
    };
    private static final String CODES = "0123456789abcdef";

    private static LegacyComponentSerializer buildSerializer() {
        final LegacyComponentSerializer.Builder builder = LegacyComponentSerializer.builder()
                .character('§')
                .hexCharacter('#')
                .useUnusualXRepeatedCharacterHexFormat();

        return builder.hexColors().build();
    }

    @NotNull
    public static Component component(@Nullable String input) {
        if (input == null) return Component.empty();
        return MINI.deserialize(toMiniMessage(input));
    }

    @NotNull
    public static String legacy(@Nullable String input) {
        if (input == null || input.isEmpty()) return "";
        return LEGACY.serialize(component(input));
    }

    @NotNull
    public static String strip(@Nullable String input) {
        if (input == null || input.isEmpty()) return "";
        return STRIP.matcher(legacy(input)).replaceAll("");
    }

    private static String toMiniMessage(String input) {
        if (input.isEmpty()) return "";

        String work = normalizeBungeeHex(input);
        work = expandHex(work);

        final StringBuilder out = new StringBuilder(work.length() + 16);
        final char[] chars = work.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            final char c = chars[i];
            if ((c == '&' || c == '§') && i + 1 < chars.length) {
                final String tag = translate(Character.toLowerCase(chars[i + 1]));
                if (tag != null) {
                    out.append(tag);
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String expandHex(String input) {
        if (input.indexOf('#') < 0) return input;

        final Matcher matcher = HEX.matcher(input);
        final StringBuffer buffer = new StringBuffer(input.length() + 16);
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement("<reset><#" + matcher.group(1) + ">"));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String normalizeBungeeHex(String input) {
        if (input.indexOf('&') < 0) return input;

        final Matcher matcher = BUNGEE_HEX.matcher(input);
        final StringBuffer buffer = new StringBuffer(input.length());
        while (matcher.find()) {
            final String raw = matcher.group();
            final StringBuilder hex = new StringBuilder(8).append("&#");
            for (int i = 3; i < raw.length(); i += 2) hex.append(raw.charAt(i));
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(hex.toString()));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String translate(char code) {
        final int index = CODES.indexOf(code);
        if (index != -1) return "<reset><" + NAMED[index] + ">";

        switch (code) {
            case 'l':
                return "<bold>";
            case 'o':
                return "<italic>";
            case 'n':
                return "<underlined>";
            case 'm':
                return "<strikethrough>";
            case 'k':
                return "<obfuscated>";
            case 'r':
                return "<reset>";
            default:
                return null;
        }
    }
}
