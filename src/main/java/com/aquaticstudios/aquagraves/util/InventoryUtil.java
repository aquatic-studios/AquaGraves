package com.aquaticstudios.aquagraves.util;

import com.aquaticstudios.aquagraves.config.GraveConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class InventoryUtil {
    public static final int MAX_ROWS = 6;
    private static final Vector ZERO = new Vector(0, 0, 0);

    private static final Set<Material> HELMETS = materials(
            "LEATHER_HELMET", "CHAINMAIL_HELMET", "IRON_HELMET", "COPPER_HELMET", "GOLDEN_HELMET",
            "DIAMOND_HELMET", "NETHERITE_HELMET", "TURTLE_HELMET");
    private static final Set<Material> CHESTPLATES = materials(
            "LEATHER_CHESTPLATE", "CHAINMAIL_CHESTPLATE", "IRON_CHESTPLATE", "COPPER_CHESTPLATE",
            "GOLDEN_CHESTPLATE", "DIAMOND_CHESTPLATE", "NETHERITE_CHESTPLATE", "ELYTRA");
    private static final Set<Material> LEGGINGS = materials(
            "LEATHER_LEGGINGS", "CHAINMAIL_LEGGINGS", "IRON_LEGGINGS", "COPPER_LEGGINGS",
            "GOLDEN_LEGGINGS", "DIAMOND_LEGGINGS", "NETHERITE_LEGGINGS");
    private static final Set<Material> BOOTS = materials(
            "LEATHER_BOOTS", "CHAINMAIL_BOOTS", "IRON_BOOTS", "COPPER_BOOTS", "GOLDEN_BOOTS",
            "DIAMOND_BOOTS", "NETHERITE_BOOTS");

    public static boolean isEmpty(@Nullable ItemStack item) {
        return item == null || item.getAmount() <= 0 || item.getType().isAir();
    }

    public static int requiredRows(int items) {
        int rows = items / 9;
        if (items % 9 != 0) rows++;
        return Math.min(MAX_ROWS, Math.max(1, rows));
    }

    @NotNull
    public static List<ItemStack> reorder(@NotNull PlayerInventory inventory,
                                          @NotNull List<ItemStack> items,
                                          @NotNull GraveConfig config) {
        final List<String> order = config.itemOrder;
        if (order.isEmpty()) return items;

        final List<ItemStack> remaining = new ArrayList<>(items);
        final List<ItemStack> ordered = new ArrayList<>(items.size());

        for (int i = 0; i < order.size(); i++) {
            switch (order.get(i)) {
                case "ARMOR":
                    for (ItemStack armor : inventory.getArmorContents()) pull(remaining, ordered, armor);
                    break;
                case "HAND":
                    pull(remaining, ordered, inventory.getItemInMainHand());
                    break;
                case "OFFHAND":
                    pull(remaining, ordered, inventory.getItemInOffHand());
                    break;
                default:
                    break;
            }
        }

        ordered.addAll(remaining);
        return ordered;
    }

    private static void pull(List<ItemStack> remaining, List<ItemStack> ordered, ItemStack item) {
        if (isEmpty(item)) return;

        for (int i = 0; i < remaining.size(); i++) {
            if (remaining.get(i) != item) continue;
            ordered.add(remaining.remove(i));
            return;
        }

        final int index = remaining.indexOf(item);
        if (index == -1) return;
        ordered.add(remaining.remove(index));
    }

    public static boolean addOrShrink(@NotNull PlayerInventory inventory, @NotNull ItemStack item) {
        final Collection<ItemStack> leftovers = inventory.addItem(item).values();
        if (leftovers.isEmpty()) {
            item.setAmount(0);
            return true;
        }

        item.setAmount(leftovers.iterator().next().getAmount());
        return false;
    }

    public static void giveOrDrop(@NotNull Player player, @NotNull ItemStack item) {
        if (isEmpty(item)) return;
        if (addOrShrink(player.getInventory(), item)) return;
        drop(player.getLocation(), item, true);
    }

    public static void drop(@NotNull Location location, @NotNull ItemStack item, boolean velocity) {
        if (isEmpty(item)) return;
        if (location.getWorld() == null) return;

        final Item dropped = location.getWorld().dropItem(location, item);
        if (!velocity) dropped.setVelocity(ZERO);
    }

    public static boolean isHelmet(@NotNull Material material) {
        return HELMETS.contains(material);
    }

    public static boolean isChestplate(@NotNull Material material) {
        return CHESTPLATES.contains(material);
    }

    public static boolean isLeggings(@NotNull Material material) {
        return LEGGINGS.contains(material);
    }

    public static boolean isBoots(@NotNull Material material) {
        return BOOTS.contains(material);
    }

    private static Set<Material> materials(String... names) {
        final Set<Material> set = EnumSet.noneOf(Material.class);
        for (String name : names) {
            final Material material = Material.getMaterial(name);
            if (material != null) set.add(material);
        }
        return set;
    }
}
