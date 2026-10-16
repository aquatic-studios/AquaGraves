package com.aquaticstudios.aquagraves.commands.sub;

import com.aquaticstudios.aquagraves.commands.CommandContext;
import com.aquaticstudios.aquagraves.commands.Subcommand;
import com.aquaticstudios.aquagraves.io.BackupRepository;
import com.aquaticstudios.aquagraves.util.ExperienceUtil;
import com.aquaticstudios.aquagraves.util.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class RestoreCommand extends Subcommand {
    public RestoreCommand(@NotNull CommandContext context) {
        super(context);
    }

    @NotNull
    @Override
    public String name() {
        return "restore";
    }

    @NotNull
    @Override
    public String permission() {
        return "restore";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            context.messages().send(sender, "messages.missing-argument", "%value%", "player");
            return;
        }

        final UUID owner = context.lookup.resolve(args[1]);
        if (owner == null) {
            context.messages().send(sender, "messages.invalid-player", "%player%", args[1]);
            return;
        }

        int index = 1;
        if (args.length >= 3) {
            final Integer parsed = parseInt(args[2]);
            if (parsed == null) {
                context.messages().send(sender, "messages.invalid-number", "%value%", args[2]);
                return;
            }
            index = parsed;
        }

        final String ownerName = context.lookup.nameOf(owner);
        final BackupRepository backups = context.graves.backups();

        if (backups.of(owner).isEmpty()) {
            context.messages().send(sender, "messages.restore-empty", "%player%", ownerName);
            return;
        }

        Player target = Bukkit.getPlayer(owner);
        if (target == null) {
            if (!(sender instanceof Player)) {
                context.messages().send(sender, "messages.players-only");
                return;
            }
            target = (Player) sender;
        }

        final BackupRepository.Backup backup = backups.take(owner, index - 1);
        if (backup == null) {
            context.messages().send(sender, "messages.invalid-number", "%value%", Integer.toString(index));
            return;
        }

        final ItemStack[] contents = backup.items();
        int given = 0;
        boolean overflow = false;

        if (contents != null) {
            for (int i = 0; i < contents.length; i++) {
                final ItemStack item = contents[i];
                if (InventoryUtil.isEmpty(item)) continue;

                given++;
                if (InventoryUtil.addOrShrink(target.getInventory(), item)) continue;

                InventoryUtil.drop(target.getLocation(), item, true);
                overflow = true;
            }
        }

        if (backup.xp > 0) ExperienceUtil.changeExp(target, backup.xp);

        context.messages().send(sender, "messages.restore-done",
                "%count%", Integer.toString(given),
                "%player%", ownerName,
                "%target%", target.getName());

        if (overflow) context.messages().send(sender, "messages.restore-overflow");
        backups.flushIfDirty();
    }

    @NotNull
    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length != 2) return Collections.emptyList();
        return context.lookup.suggestions(args[1]);
    }
}
