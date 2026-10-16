package com.aquaticstudios.aquagraves.commands.sub;

import com.aquaticstudios.aquagraves.commands.CommandContext;
import com.aquaticstudios.aquagraves.commands.Subcommand;
import com.aquaticstudios.aquagraves.config.GraveConfig;
import com.aquaticstudios.aquagraves.grave.Grave;
import com.aquaticstudios.aquagraves.util.Permissions;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class TeleportCommand extends Subcommand {
    public TeleportCommand(@NotNull CommandContext context) {
        super(context);
    }

    @NotNull
    @Override
    public String name() {
        return "tp";
    }

    @NotNull
    @Override
    public String permission() {
        return "teleport";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            context.messages().send(sender, "messages.players-only");
            return;
        }

        final Player player = (Player) sender;
        final GraveConfig config = context.configs.config();

        if (!config.teleportEnabled) {
            context.messages().send(player, "messages.teleport-disabled");
            return;
        }

        UUID owner = player.getUniqueId();
        int index = 1;

        if (args.length >= 2) {
            final Integer parsed = parseInt(args[1]);
            if (parsed != null) {
                index = parsed;
            } else {
                final UUID resolved = context.lookup.resolve(args[1]);
                if (resolved == null) {
                    context.messages().send(player, "messages.invalid-player", "%player%", args[1]);
                    return;
                }

                if (!resolved.equals(player.getUniqueId()) && !Permissions.has(player, "teleport.others")) {
                    context.messages().send(player, "messages.no-permission");
                    return;
                }
                owner = resolved;
            }
        }

        if (args.length >= 3) {
            final Integer parsed = parseInt(args[2]);
            if (parsed == null) {
                context.messages().send(player, "messages.invalid-number", "%value%", args[2]);
                return;
            }
            index = parsed;
        }

        final boolean own = owner.equals(player.getUniqueId());
        if (!own && config.teleportOnlyOwn && !Permissions.has(player, "teleport.others")) {
            context.messages().send(player, "messages.teleport-not-yours");
            return;
        }

        final long cooldown = Permissions.canBypass(player, "cooldown")
                ? 0L
                : context.teleports.cooldowns().remaining(player.getUniqueId());

        if (cooldown > 0L) {
            context.messages().send(player, "messages.teleport-cooldown",
                    "%seconds%", Long.toString((cooldown + 999L) / 1000L));
            return;
        }

        final List<Grave> graves = context.registry.ofOwner(owner);
        if (graves.isEmpty()) {
            context.messages().send(player, "list.empty");
            return;
        }

        if (index < 1 || index > graves.size()) {
            context.messages().send(player, "messages.invalid-number", "%value%", Integer.toString(index));
            return;
        }

        context.teleports.request(player, graves.get(index - 1).location().add(0.0D, 0.5D, 0.0D));
    }

    @NotNull
    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 2 && Permissions.has(sender, "teleport.others")) {
            return context.lookup.suggestions(args[1]);
        }
        return Collections.emptyList();
    }
}
