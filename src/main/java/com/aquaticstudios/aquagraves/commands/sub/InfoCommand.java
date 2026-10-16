package com.aquaticstudios.aquagraves.commands.sub;

import com.aquaticstudios.aquagraves.commands.CommandContext;
import com.aquaticstudios.aquagraves.commands.Subcommand;
import com.aquaticstudios.aquagraves.grave.Grave;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class InfoCommand extends Subcommand {
    private static final double SEARCH_RADIUS = 8.0D;

    public InfoCommand(@NotNull CommandContext context) {
        super(context);
    }

    @NotNull
    @Override
    public String name() {
        return "info";
    }

    @NotNull
    @Override
    public String permission() {
        return "info";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            context.messages().send(sender, "messages.players-only");
            return;
        }

        final List<Grave> graves = context.registry.near(((Player) sender).getLocation(), SEARCH_RADIUS);
        if (graves.isEmpty()) {
            context.messages().send(sender, "list.empty");
            return;
        }

        context.messages().sendGraveList(sender, "info", graves.get(0));
    }
}
