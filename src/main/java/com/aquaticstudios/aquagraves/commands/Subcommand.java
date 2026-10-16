package com.aquaticstudios.aquagraves.commands;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public abstract class Subcommand {
    protected final CommandContext context;

    protected Subcommand(@NotNull CommandContext context) {
        this.context = context;
    }

    @NotNull
    public abstract String name();

    @Nullable
    public abstract String permission();

    public abstract void execute(@NotNull CommandSender sender, @NotNull String[] args);

    @NotNull
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return Collections.emptyList();
    }

    @Nullable
    protected Integer parseInt(@NotNull String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
