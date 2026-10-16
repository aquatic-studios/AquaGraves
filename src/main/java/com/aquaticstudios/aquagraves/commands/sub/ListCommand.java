package com.aquaticstudios.aquagraves.commands.sub;

import com.aquaticstudios.aquagraves.commands.CommandContext;
import com.aquaticstudios.aquagraves.commands.Subcommand;
import com.aquaticstudios.aquagraves.config.template.GraveTemplate;
import com.aquaticstudios.aquagraves.grave.Grave;
import com.aquaticstudios.aquagraves.util.Permissions;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class ListCommand extends Subcommand {
    private static final int PER_PAGE = 8;

    public ListCommand(@NotNull CommandContext context) {
        super(context);
    }

    @NotNull
    @Override
    public String name() {
        return "list";
    }

    @NotNull
    @Override
    public String permission() {
        return "list";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        UUID owner = null;
        int page = 1;

        if (args.length >= 2) {
            final Integer parsed = parseInt(args[1]);
            if (parsed != null) {
                page = parsed;
            } else {
                if (!Permissions.has(sender, "list.others")) {
                    context.messages().send(sender, "messages.no-permission");
                    return;
                }

                owner = context.lookup.resolve(args[1]);
                if (owner == null) {
                    context.messages().send(sender, "messages.invalid-player", "%player%", args[1]);
                    return;
                }
            }
        }

        if (args.length >= 3) {
            final Integer parsed = parseInt(args[2]);
            if (parsed == null) {
                context.messages().send(sender, "messages.invalid-number", "%value%", args[2]);
                return;
            }
            page = parsed;
        }

        if (owner == null) {
            if (!(sender instanceof Player)) {
                context.messages().send(sender, "messages.players-only");
                return;
            }
            owner = ((Player) sender).getUniqueId();
        }

        final List<Grave> graves = context.registry.ofOwner(owner);
        if (graves.isEmpty()) {
            context.messages().send(sender, "list.empty");
            return;
        }

        final int pages = (graves.size() + PER_PAGE - 1) / PER_PAGE;
        if (page < 1) page = 1;
        if (page > pages) page = pages;

        final String ownerName = context.lookup.nameOf(owner);

        context.messages().send(sender, "list.header",
                "%player%", ownerName,
                "%page%", Integer.toString(page),
                "%pages%", Integer.toString(pages),
                "%count%", Integer.toString(graves.size()));

        final int from = (page - 1) * PER_PAGE;
        final int to = Math.min(graves.size(), from + PER_PAGE);

        for (int i = from; i < to; i++) {
            entry(context, sender, graves.get(i), ownerName, i + 1);
        }

        context.messages().send(sender, "list.footer");
    }

    public static void entry(@NotNull CommandContext context, @NotNull CommandSender sender,
                             @NotNull Grave grave, @NotNull String ownerName, int index) {
        final GraveTemplate template = context.messages().template("list.entry");
        if (template == null) return;

        final String line = template.render(grave).replace("%index%", Integer.toString(index));

        if (!(sender instanceof Player)) {
            sender.sendMessage(line);
            return;
        }

        final boolean own = grave.ownerId().equals(((Player) sender).getUniqueId());
        final String command = own
                ? "/aquagraves tp " + index
                : "/aquagraves tp " + ownerName + " " + index;

        final TextComponent component = new TextComponent(TextComponent.fromLegacyText(line));
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));

        final GraveTemplate hoverTemplate = context.messages().template("list.entry-hover");
        if (hoverTemplate != null) {
            final BaseComponent[] hover = TextComponent.fromLegacyText(
                    hoverTemplate.render(grave).replace("%index%", Integer.toString(index)));
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover));
        }

        ((Player) sender).spigot().sendMessage(component);
    }

    @NotNull
    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 2 && Permissions.has(sender, "list.others")) {
            return context.lookup.suggestions(args[1]);
        }
        return Collections.emptyList();
    }
}
