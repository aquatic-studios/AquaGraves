package com.aquaticstudios.aquagraves.commands.sub;

import com.aquaticstudios.aquagraves.commands.CommandContext;
import com.aquaticstudios.aquagraves.commands.Subcommand;
import com.aquaticstudios.aquagraves.grave.Grave;
import com.aquaticstudios.aquagraves.grave.RemoveReason;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class RemoveCommand extends Subcommand {
    private static final double SEARCH_RADIUS = 8.0D;

    public RemoveCommand(@NotNull CommandContext context) {
        super(context);
    }

    @NotNull
    @Override
    public String name() {
        return "remove";
    }

    @NotNull
    @Override
    public String permission() {
        return "remove";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length >= 2) {
            if (args[1].equalsIgnoreCase("all")) {
                final int removed = context.graves.removeAll(RemoveReason.COMMAND);
                context.messages().send(sender, "messages.grave-removed", "%count%", Integer.toString(removed));
                return;
            }

            final UUID owner = context.lookup.resolve(args[1]);
            if (owner == null) {
                context.messages().send(sender, "messages.invalid-player", "%player%", args[1]);
                return;
            }

            final int removed = context.graves.removeAllOf(owner, RemoveReason.COMMAND);
            context.messages().send(sender, "messages.grave-removed", "%count%", Integer.toString(removed));
            return;
        }

        if (!(sender instanceof Player)) {
            context.messages().send(sender, "messages.missing-argument", "%value%", "player");
            return;
        }

        final List<Grave> graves = context.registry.near(((Player) sender).getLocation(), SEARCH_RADIUS);
        if (graves.isEmpty()) {
            context.messages().send(sender, "list.empty");
            return;
        }

        graves.get(0).remove(RemoveReason.COMMAND);
        context.messages().send(sender, "messages.grave-removed", "%count%", "1");
    }

    @NotNull
    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length != 2) return new ArrayList<>();

        final List<String> results = new ArrayList<>(context.lookup.suggestions(args[1]));
        if ("all".startsWith(args[1].toLowerCase())) results.add(0, "all");
        return results;
    }
}
