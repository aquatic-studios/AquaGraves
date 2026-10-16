package com.aquaticstudios.aquagraves.hologram.impl;

import com.aquaticstudios.aquagraves.config.GraveConfig;
import com.aquaticstudios.aquagraves.config.template.GraveField;
import com.aquaticstudios.aquagraves.config.template.GraveTemplate;
import com.aquaticstudios.aquagraves.grave.GraveEntities;
import com.aquaticstudios.aquagraves.hologram.Hologram;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class ArmorStandHologram implements Hologram {
    private final UUID graveId;
    private final GraveConfig config;
    private final StringBuilder scratch = new StringBuilder(64);

    private ArmorStand[] stands = new ArmorStand[0];
    private String[] rendered = new String[0];
    private Location anchor;

    public ArmorStandHologram(@NotNull UUID graveId, @NotNull GraveConfig config) {
        this.graveId = graveId;
        this.config = config;
    }

    @Override
    public void spawn(@NotNull Location location, @NotNull GraveField.Source source, @NotNull GraveTemplate[] lines) {
        remove();

        this.anchor = location.clone();

        final World world = location.getWorld();
        if (world == null) return;

        this.stands = new ArmorStand[lines.length];
        this.rendered = new String[lines.length];

        for (int i = 0; i < lines.length; i++) {
            scratch.setLength(0);
            lines[i].render(source, scratch);

            final String text = scratch.toString();
            rendered[i] = text;

            final Location at = location.clone().add(
                    0.0D, (lines.length - 1 - i) * config.hologramLineHeight, 0.0D);

            stands[i] = world.spawn(at, ArmorStand.class, entity -> {
                entity.setVisible(false);
                entity.setGravity(false);
                entity.setMarker(true);
                entity.setSmall(true);
                entity.setBasePlate(false);
                entity.setArms(false);
                entity.setSilent(true);
                entity.setInvulnerable(true);
                entity.setCanPickupItems(false);
                entity.setPersistent(false);
                entity.setRemoveWhenFarAway(false);
                entity.setCustomNameVisible(!text.isEmpty());
                entity.setCustomName(text);
                GraveEntities.tag(entity, graveId);
            });
        }
    }

    @Override
    public void update(@NotNull GraveField.Source source, @NotNull GraveTemplate[] lines) {
        if (anchor == null) return;

        if (lines.length != stands.length || !isAlive()) {
            spawn(anchor, source, lines);
            return;
        }

        for (int i = 0; i < lines.length; i++) {
            if (lines[i].isStatic()) continue;

            scratch.setLength(0);
            lines[i].render(source, scratch);

            final String previous = rendered[i];
            if (previous != null && previous.contentEquals(scratch)) continue;

            final String text = scratch.toString();
            rendered[i] = text;

            final ArmorStand stand = stands[i];
            stand.setCustomName(text);
            stand.setCustomNameVisible(!text.isEmpty());
        }
    }

    @Override
    public void remove() {
        for (int i = 0; i < stands.length; i++) {
            if (stands[i] == null) continue;
            stands[i].remove();
        }
        stands = new ArmorStand[0];
        rendered = new String[0];
    }

    @Override
    public boolean isSpawned() {
        return stands.length > 0 && isAlive();
    }

    private boolean isAlive() {
        for (int i = 0; i < stands.length; i++) {
            if (stands[i] == null || !stands[i].isValid()) return false;
        }
        return true;
    }
}
