package com.aquaticstudios.aquagraves.util;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class Permissions {
    public static final String ROOT = "aquagraves";

    private static final String ADMIN = ROOT + ".admin";
    private static final String WILDCARD = ROOT + ".*";
    private static final String USE = ROOT + ".use";
    private static final String BYPASS_ALL = ROOT + ".bypass.*";

    public static boolean has(@NotNull CommandSender sender, @NotNull String node) {
        return sender.hasPermission(ADMIN)
                || sender.hasPermission(ROOT + "." + node)
                || sender.hasPermission(WILDCARD);
    }

    public static boolean isAdmin(@NotNull CommandSender sender) {
        return sender.hasPermission(ADMIN) || sender.hasPermission(WILDCARD);
    }

    public static boolean canHaveGraves(@NotNull CommandSender sender) {
        return sender.hasPermission(USE);
    }

    public static boolean canBypass(@NotNull CommandSender sender, @NotNull String node) {
        return sender.hasPermission(ROOT + ".bypass." + node)
                || sender.hasPermission(BYPASS_ALL)
                || sender.hasPermission(ADMIN);
    }
}
