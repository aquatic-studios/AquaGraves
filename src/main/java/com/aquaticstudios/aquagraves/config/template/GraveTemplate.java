package com.aquaticstudios.aquagraves.config.template;

import com.aquaticstudios.aquagraves.util.CC;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class GraveTemplate {
    private static final char MARKER = '\0';

    private final String[] literals;
    private final int[] fields;

    private GraveTemplate(String[] literals, int[] fields) {
        this.literals = literals;
        this.fields = fields;
    }

    @NotNull
    public static GraveTemplate compile(@NotNull String raw) {
        return compile(raw, false);
    }

    @NotNull
    public static GraveTemplate compile(@NotNull String raw, boolean nested) {
        String work = raw;

        for (int field = 0; field < GraveField.COUNT; field++) {
            if (nested && field == GraveField.LOCK_STATUS) continue;

            final String token = GraveField.token(field);
            if (work.indexOf(token) < 0) continue;

            work = work.replace(token, marker(field));
        }

        work = work.replace("%owner%", marker(GraveField.PLAYER));

        return split(CC.legacy(work));
    }

    @NotNull
    public static GraveTemplate[] compileAll(@NotNull List<String> lines) {
        final GraveTemplate[] compiled = new GraveTemplate[lines.size()];
        for (int i = 0; i < compiled.length; i++) {
            compiled[i] = compile(lines.get(i));
        }
        return compiled;
    }

    public void render(@NotNull GraveField.Source source, @NotNull StringBuilder out) {
        out.append(literals[0]);
        for (int i = 0; i < fields.length; i++) {
            source.appendField(fields[i], out);
            out.append(literals[i + 1]);
        }
    }

    @NotNull
    public String render(@NotNull GraveField.Source source) {
        if (fields.length == 0) return literals[0];

        final StringBuilder out = new StringBuilder(literals[0].length() + 32);
        render(source, out);
        return out.toString();
    }

    public boolean isStatic() {
        return fields.length == 0;
    }

    private static String marker(int field) {
        return new String(new char[]{MARKER, (char) ('a' + field), MARKER});
    }

    private static GraveTemplate split(String formatted) {
        final List<String> literals = new ArrayList<>(4);
        final List<Integer> fields = new ArrayList<>(4);

        final int length = formatted.length();
        int cursor = 0;

        while (true) {
            final int start = nextMarker(formatted, cursor, length);
            if (start < 0) {
                literals.add(formatted.substring(cursor));
                break;
            }

            literals.add(formatted.substring(cursor, start));
            fields.add(formatted.charAt(start + 1) - 'a');
            cursor = start + 3;
        }

        final int[] fieldArray = new int[fields.size()];
        for (int i = 0; i < fieldArray.length; i++) fieldArray[i] = fields.get(i);

        return new GraveTemplate(literals.toArray(new String[0]), fieldArray);
    }

    private static int nextMarker(String text, int from, int length) {
        for (int i = from; i + 2 < length; i++) {
            if (text.charAt(i) != MARKER) continue;
            if (text.charAt(i + 2) != MARKER) continue;

            final int field = text.charAt(i + 1) - 'a';
            if (field < 0 || field >= GraveField.COUNT) continue;

            return i;
        }
        return -1;
    }
}
