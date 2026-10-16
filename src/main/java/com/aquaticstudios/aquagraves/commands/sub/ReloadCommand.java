package com.aquaticstudios.aquagraves.commands.sub;

import com.aquaticstudios.aquagraves.commands.CommandContext;
import com.aquaticstudios.aquagraves.commands.Subcommand;
import com.aquaticstudios.aquagraves.grave.Grave;
import com.aquaticstudios.aquagraves.grave.GraveTick;
import com.aquaticstudios.aquagraves.util.SkullUtil;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ReloadCommand extends Subcommand {
    public ReloadCommand(@NotNull CommandContext context) {
        super(context);
    }

    @NotNull
    @Override
    public String name() {
        return "reload";
    }

    @Nullable
    @Override
    public String permission() {
        return "reload";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        context.graves.repository().flush();

        if (!context.configs.reload()) {
            context.messages().send(sender, "messages.reload-failed");
            return;
        }

        context.graves.permissions().clear();
        SkullUtil.invalidate();

        final GraveTick pass = GraveTick.immediate(context.configs.config(), context.configs.messages());
        for (Grave grave : context.registry.all()) {
            if (grave.isRemoved()) continue;

            context.graves.scheduler().execute(grave.locationRef(), () -> {
                grave.despawnEntities();
                grave.refreshVisuals(pass);
            });
        }

        context.ticker.restart();
        context.messages().send(sender, "messages.reload");
    }
}
