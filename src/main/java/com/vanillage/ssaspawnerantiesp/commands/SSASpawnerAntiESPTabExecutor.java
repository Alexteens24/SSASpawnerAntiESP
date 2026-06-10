package com.vanillage.ssaspawnerantiesp.commands;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import com.vanillage.ssaspawnerantiesp.SSASpawnerAntiESP;

public final class SSASpawnerAntiESPTabExecutor implements CommandExecutor, TabCompleter {
    private final SSASpawnerAntiESP plugin;

    public SSASpawnerAntiESPTabExecutor(SSASpawnerAntiESP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ssaspawnerantiesp.command.reload")) {
            sender.sendMessage("§cYou don't have permissions.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPluginConfiguration();
            sender.sendMessage("§aSSASpawnerAntiESP configuration reloaded.");
            return true;
        }

        sender.sendMessage("§cUsage: /" + label + " reload");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Collections.singletonList("reload");
        }

        return Collections.emptyList();
    }
}
