package com.aquaticstudios.aquagraves.commands;

import com.aquaticstudios.aquagraves.util.Permissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CommandRegistrar {
    private final Plugin plugin;

    private GravesCommand command;
    private CommandMap commandMap;

    public CommandRegistrar(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    public void register(@NotNull CommandContext context, @NotNull List<String> aliases) {
        registerPermissions();

        commandMap = resolveCommandMap();
        if (commandMap == null) {
            plugin.getLogger().severe("Could not access the server command map, the command was not registered");
            return;
        }

        final List<String> labels = new ArrayList<>(aliases);
        final String label = labels.remove(0);

        command = new GravesCommand(label, labels, context);
        commandMap.register(plugin.getName().toLowerCase(), command);
    }

    public void unregister() {
        if (command == null || commandMap == null) return;

        command.unregister(commandMap);

        try {
            final Field field = SimpleCommandMap.class.getDeclaredField("knownCommands");
            field.setAccessible(true);

            @SuppressWarnings("unchecked")
            final Map<String, Command> known = (Map<String, Command>) field.get(commandMap);
            known.values().removeIf(entry -> entry == command);
        } catch (ReflectiveOperationException ignored) {
        }

        command = null;
        commandMap = null;
    }

    private CommandMap resolveCommandMap() {
        try {
            final Method method = plugin.getServer().getClass().getMethod("getCommandMap");
            return (CommandMap) method.invoke(plugin.getServer());
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            final Field field = plugin.getServer().getPluginManager().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            return (CommandMap) field.get(plugin.getServer().getPluginManager());
        } catch (ReflectiveOperationException ignored) {
        }

        return null;
    }

    private void registerPermissions() {
        final String root = Permissions.ROOT;
        final Permission admin = define(root + ".admin", PermissionDefault.OP);

        child(admin, define(root + ".use", PermissionDefault.TRUE));
        child(admin, define(root + ".teleport", PermissionDefault.TRUE));
        child(admin, define(root + ".teleport.others", PermissionDefault.OP));
        child(admin, define(root + ".list", PermissionDefault.TRUE));
        child(admin, define(root + ".list.others", PermissionDefault.OP));
        child(admin, define(root + ".near", PermissionDefault.OP));
        child(admin, define(root + ".info", PermissionDefault.OP));
        child(admin, define(root + ".remove", PermissionDefault.OP));
        child(admin, define(root + ".restore", PermissionDefault.OP));
        child(admin, define(root + ".purge", PermissionDefault.OP));
        child(admin, define(root + ".stats", PermissionDefault.OP));
        child(admin, define(root + ".reload", PermissionDefault.OP));
        child(admin, define(root + ".update", PermissionDefault.OP));

        final Permission bypass = define(root + ".bypass.*", PermissionDefault.OP);
        child(bypass, define(root + ".bypass.protection", PermissionDefault.OP));
        child(bypass, define(root + ".bypass.cooldown", PermissionDefault.OP));
        bypass.recalculatePermissibles();

        child(admin, bypass);
        admin.recalculatePermissibles();
    }

    private void child(Permission parent, Permission child) {
        parent.getChildren().put(child.getName(), true);
    }

    private Permission define(String node, PermissionDefault fallback) {
        final PluginManager manager = plugin.getServer().getPluginManager();

        Permission permission = manager.getPermission(node);
        if (permission != null) return permission;

        permission = new Permission(node, fallback);
        manager.addPermission(permission);
        return permission;
    }
}
