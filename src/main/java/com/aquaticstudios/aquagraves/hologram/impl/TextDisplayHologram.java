package com.aquaticstudios.aquagraves.hologram.impl;

import com.aquaticstudios.aquagraves.config.GraveConfig;
import com.aquaticstudios.aquagraves.config.template.GraveField;
import com.aquaticstudios.aquagraves.config.template.GraveTemplate;
import com.aquaticstudios.aquagraves.grave.GraveEntities;
import com.aquaticstudios.aquagraves.hologram.Hologram;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class TextDisplayHologram implements Hologram {
    private final UUID graveId;
    private final GraveConfig config;
    private final StringBuilder scratch = new StringBuilder(128);

    private TextDisplay display;
    private String rendered;
    private Location anchor;

    public TextDisplayHologram(@NotNull UUID graveId, @NotNull GraveConfig config) {
        this.graveId = graveId;
        this.config = config;
    }

    @Override
    public void spawn(@NotNull Location location, @NotNull GraveField.Source source, @NotNull GraveTemplate[] lines) {
        remove();

        this.anchor = location.clone();

        final World world = location.getWorld();
        if (world == null) return;

        render(source, lines);
        final String text = scratch.toString();
        this.rendered = text;

        final Location at = location.clone().add(0.0D, Math.max(0, lines.length - 1) * 0.125D, 0.0D);

        this.display = world.spawn(at, TextDisplay.class, entity -> {
            entity.setText(text);
            entity.setBillboard(billboard());
            entity.setAlignment(alignment());
            entity.setSeeThrough(config.hologramSeeThrough);
            entity.setShadowed(config.hologramShadow);
            entity.setLineWidth(1000);
            entity.setBackgroundColor(background());
            entity.setPersistent(false);
            entity.setInvulnerable(true);
            entity.setViewRange((float) Math.max(0.1D, config.hologramViewDistance / 64.0D));
            GraveEntities.tag(entity, graveId);
        });
    }

    @Override
    public void update(@NotNull GraveField.Source source, @NotNull GraveTemplate[] lines) {
        if (anchor == null) return;

        if (display == null || !display.isValid()) {
            spawn(anchor, source, lines);
            return;
        }

        render(source, lines);
        if (rendered != null && rendered.contentEquals(scratch)) return;

        rendered = scratch.toString();
        display.setText(rendered);
    }

    @Override
    public void remove() {
        if (display != null) display.remove();
        display = null;
        rendered = null;
    }

    @Override
    public boolean isSpawned() {
        return display != null && display.isValid();
    }

    private void render(GraveField.Source source, GraveTemplate[] lines) {
        scratch.setLength(0);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) scratch.append('\n');
            lines[i].render(source, scratch);
        }
    }

    private Color background() {
        final int argb = config.hologramBackground;
        return Color.fromARGB((argb >>> 24) & 0xFF, (argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF);
    }

    private Display.Billboard billboard() {
        try {
            return Display.Billboard.valueOf(config.hologramBillboard);
        } catch (IllegalArgumentException exception) {
            return Display.Billboard.VERTICAL;
        }
    }

    private TextDisplay.TextAlignment alignment() {
        try {
            return TextDisplay.TextAlignment.valueOf(config.hologramAlignment);
        } catch (IllegalArgumentException exception) {
            return TextDisplay.TextAlignment.CENTER;
        }
    }
}
