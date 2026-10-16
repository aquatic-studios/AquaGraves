package com.aquaticstudios.aquagraves.commands.sub;

import com.aquaticstudios.aquagraves.commands.CommandContext;
import com.aquaticstudios.aquagraves.commands.Subcommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class HelpCommand extends Subcommand {
    public HelpCommand(@NotNull CommandContext context) {
        super(context);
    }

    @NotNull
    @Override
    public String name() {
        return "help";
    }

    @Nullable
    @Override
    public String permission() {
        return null;
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        context.messages().sendList(sender, "help");
    }
}
