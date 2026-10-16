package com.aquaticstudios.aquagraves.commands.sub;

import com.aquaticstudios.aquagraves.commands.CommandContext;
import com.aquaticstudios.aquagraves.commands.Subcommand;
import com.aquaticstudios.aquagraves.grave.Grave;
import com.aquaticstudios.aquagraves.hologram.HologramFactory;
import com.aquaticstudios.aquagraves.platform.Platform;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class StatsCommand extends Subcommand {
    public StatsCommand(@NotNull CommandContext context) {
        super(context);
    }

    @NotNull
    @Override
    public String name() {
        return "stats";
    }

    @NotNull
    @Override
    public String permission() {
        return "stats";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        final long now = System.currentTimeMillis();

        int stacks = 0;
        int xp = 0;
        int locked = 0;

        for (Grave grave : context.registry.all()) {
            stacks += grave.stackCount();
            xp += grave.storedXp();
            if (grave.isProtected(now)) locked++;
        }

        context.messages().sendList(sender, "stats",
                "%graves%", Integer.toString(context.registry.size()),
                "%owners%", Integer.toString(context.registry.ownerCount()),
                "%items%", Integer.toString(stacks),
                "%xp%", Integer.toString(xp),
                "%protected%", Integer.toString(locked),
                "%backups%", Integer.toString(context.graves.backups().total()),
                "%hologram-type%", HologramFactory.describe(context.configs.config()),
                "%platform%", Platform.current().name(),
                "%ticker%", context.ticker.isRunning() ? "running" : "idle",
                "%permission-cache%", Integer.toString(context.graves.permissions().size()));
    }
}
