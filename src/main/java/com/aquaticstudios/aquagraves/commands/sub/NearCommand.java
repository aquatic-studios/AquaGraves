package com.aquaticstudios.aquagraves.commands.sub;

import com.aquaticstudios.aquagraves.commands.CommandContext;
import com.aquaticstudios.aquagraves.commands.Subcommand;
import com.aquaticstudios.aquagraves.grave.Grave;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class NearCommand extends Subcommand {
    private static final double DEFAULT_RADIUS = 32.0D;
    private static final double MAX_RADIUS = 512.0D;

    public NearCommand(@NotNull CommandContext context) {
        super(context);
    }

    @NotNull
    @Override
    public String name() {
        return "near";
    }

    @NotNull
    @Override
    public String permission() {
        return "near";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            context.messages().send(sender, "messages.players-only");
            return;
        }

        final Player player = (Player) sender;
        double radius = DEFAULT_RADIUS;

        if (args.length >= 2) {
            try {
                radius = Double.parseDouble(args[1]);
            } catch (NumberFormatException exception) {
                context.messages().send(sender, "messages.invalid-number", "%value%", args[1]);
                return;
            }
            radius = Math.min(MAX_RADIUS, Math.max(1.0D, radius));
        }

        final List<Grave> graves = context.registry.near(player.getLocation(), radius);
        if (graves.isEmpty()) {
            context.messages().send(sender, "list.empty");
            return;
        }

        context.messages().send(sender, "list.header",
                "%player%", ((int) radius) + " blocks",
                "%page%", "1",
                "%pages%", "1",
                "%count%", Integer.toString(graves.size()));

        for (int i = 0; i < graves.size(); i++) {
            final Grave grave = graves.get(i);
            final int index = context.registry.ofOwner(grave.ownerId()).indexOf(grave) + 1;
            ListCommand.entry(context, sender, grave, grave.ownerName(), Math.max(1, index));
        }

        context.messages().send(sender, "list.footer");
    }

    @NotNull
    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length != 2) return Collections.emptyList();
        return Arrays.asList("16", "32", "64", "128");
    }
}
