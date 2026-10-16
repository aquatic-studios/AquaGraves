package com.aquaticstudios.aquagraves.util;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ExperienceUtil {
    public static int getExp(@NotNull Player player) {
        return getExpFromLevel(player.getLevel()) + Math.round(getExpToNext(player.getLevel()) * player.getExp());
    }

    public static int getExpFromLevel(int level) {
        if (level > 30) return (int) (4.5D * level * level - 162.5D * level + 2220.0D);
        if (level > 15) return (int) (2.5D * level * level - 40.5D * level + 360.0D);
        return level * level + 6 * level;
    }

    public static double getLevelFromExp(long exp) {
        final int level = getIntLevelFromExp(exp);
        final float remainder = exp - (float) getExpFromLevel(level);
        return level + (remainder / getExpToNext(level));
    }

    public static int getIntLevelFromExp(long exp) {
        if (exp > 1395L) return (int) ((Math.sqrt(72L * exp - 54215D) + 325D) / 18D);
        if (exp > 315L) return (int) (Math.sqrt(40L * exp - 7839D) / 10D + 8.1D);
        if (exp > 0L) return (int) (Math.sqrt(exp + 9D) - 3D);
        return 0;
    }

    public static int getExpToNext(int level) {
        if (level >= 30) return level * 9 - 158;
        if (level >= 15) return level * 5 - 38;
        return level * 2 + 7;
    }

    public static void changeExp(@NotNull Player player, int amount) {
        int exp = getExp(player) + amount;
        if (exp < 0) exp = 0;

        final double levelAndExp = getLevelFromExp(exp);
        final int level = (int) levelAndExp;

        player.setLevel(level);
        player.setExp((float) (levelAndExp - level));
    }
}
