package com.aquaticstudios.aquagraves.util;

import com.aquaticstudios.aquagraves.config.GraveConfig;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public final class ItemFilter {
    public static boolean isDenied(@Nullable ItemStack item, @NotNull GraveConfig config) {
        if (InventoryUtil.isEmpty(item)) return false;
        if (config.deniedMaterials.contains(item.getType())) return true;

        final List<String> names = config.deniedNames;
        if (names.isEmpty()) return false;
        if (!item.hasItemMeta()) return false;

        final ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;

        final String display = CC.strip(meta.getDisplayName()).toLowerCase(Locale.ROOT);
        for (int i = 0; i < names.size(); i++) {
            if (display.contains(names.get(i))) return true;
        }
        return false;
    }
}
