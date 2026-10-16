package com.aquaticstudios.aquagraves.commands;

import com.aquaticstudios.aquagraves.commands.sub.HelpCommand;
import com.aquaticstudios.aquagraves.commands.sub.InfoCommand;
import com.aquaticstudios.aquagraves.commands.sub.ListCommand;
import com.aquaticstudios.aquagraves.commands.sub.NearCommand;
import com.aquaticstudios.aquagraves.commands.sub.PurgeCommand;
import com.aquaticstudios.aquagraves.commands.sub.ReloadCommand;
import com.aquaticstudios.aquagraves.commands.sub.RemoveCommand;
import com.aquaticstudios.aquagraves.commands.sub.RestoreCommand;
import com.aquaticstudios.aquagraves.commands.sub.StatsCommand;
import com.aquaticstudios.aquagraves.commands.sub.TeleportCommand;
import com.aquaticstudios.aquagraves.platform.Platform;
import com.aquaticstudios.aquagraves.util.CC;
import com.aquaticstudios.aquagraves.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GravesCommand extends Command {
    private final CommandContext context;
    private final Map<String, Subcommand> subcommands = new LinkedHashMap<>();

    private final String[] banner;

    public GravesCommand(@NotNull String label, @NotNull List<String> aliases, @NotNull CommandContext context) {
        super(label);
        setAliases(aliases);
        setDescription("Main command.");

        this.context = context;

        register(new HelpCommand(context));
        register(new TeleportCommand(context));
        register(new ListCommand(context));
        register(new NearCommand(context));
        register(new InfoCommand(context));
        register(new RemoveCommand(context));
        register(new RestoreCommand(context));
        register(new PurgeCommand(context));
        register(new StatsCommand(context));
        register(new ReloadCommand(context));

        final String version = context.plugin.getDescription().getVersion();
        final String server = Bukkit.getBukkitVersion().split("-")[0];

        this.banner = new String[]{
                CC.legacy(" "),
                CC.legacy("           &#33CCFF&lAquaGraves &fversion &#8DFF87[" + version + "] &7("
                        + server + ")"),
                CC.legacy("           &fPowered by &#8BD5FFSenkex @ Aquatic Studios"),
                CC.legacy(" ")
        };
    }

    private void register(@NotNull Subcommand subcommand) {
        subcommands.put(subcommand.name(), subcommand);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            for (int i = 0; i < banner.length; i++) sender.sendMessage(banner[i]);
            return true;
        }

        final Subcommand subcommand = subcommands.get(args[0].toLowerCase());
        if (subcommand == null) {
            context.messages().send(sender, "messages.unknown-command");
            return true;
        }

        if (subcommand.permission() != null && !Permissions.has(sender, subcommand.permission())) {
            context.messages().send(sender, "messages.no-permission");
            return true;
        }

        subcommand.execute(sender, args);
        return true;
    }

    @NotNull
    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 0) return Collections.emptyList();

        if (args.length == 1) {
            final String typed = args[0].toLowerCase();
            final List<String> results = new ArrayList<>(subcommands.size());

            for (Map.Entry<String, Subcommand> entry : subcommands.entrySet()) {
                if (!entry.getKey().startsWith(typed)) continue;

                final Subcommand subcommand = entry.getValue();
                if (subcommand.permission() != null && !Permissions.has(sender, subcommand.permission())) continue;

                results.add(entry.getKey());
            }
            return results;
        }

        final Subcommand subcommand = subcommands.get(args[0].toLowerCase());
        if (subcommand == null) return Collections.emptyList();
        if (subcommand.permission() != null && !Permissions.has(sender, subcommand.permission())) {
            return Collections.emptyList();
        }

        return subcommand.tabComplete(sender, args);
    }
}
