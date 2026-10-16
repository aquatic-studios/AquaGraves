package com.aquaticstudios.aquagraves.commands.sub;

import com.aquaticstudios.aquagraves.commands.CommandContext;
import com.aquaticstudios.aquagraves.commands.Subcommand;
import com.aquaticstudios.aquagraves.grave.RemoveReason;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PurgeCommand extends Subcommand {
    private static final long CONFIRM_WINDOW = 30_000L;

    private final Map<String, Long> pending = new ConcurrentHashMap<>();

    public PurgeCommand(@NotNull CommandContext context) {
        super(context);
    }

    @NotNull
    @Override
    public String name() {
        return "purge";
    }

    @NotNull
    @Override
    public String permission() {
        return "purge";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        final String key = sender.getName();

        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            pending.put(key, System.currentTimeMillis() + CONFIRM_WINDOW);
            context.messages().send(sender, "messages.purge-warning");
            return;
        }

        final Long until = pending.remove(key);
        if (until == null || until < System.currentTimeMillis()) {
            context.messages().send(sender, "messages.purge-nothing");
            return;
        }

        final int removed = context.graves.removeAll(RemoveReason.PURGE);
        context.graves.repository().flush();

        context.messages().send(sender, "messages.purge-done", "%count%", Integer.toString(removed));
    }

    @NotNull
    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length != 2) return Collections.emptyList();
        return Collections.singletonList("confirm");
    }
}
