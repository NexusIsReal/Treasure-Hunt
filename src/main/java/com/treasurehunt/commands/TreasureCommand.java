package com.treasurehunt.commands;

import com.treasurehunt.TreasureHuntPlugin;
import com.treasurehunt.managers.TreasureManager;
import com.treasurehunt.gui.TreasureGUI;
import com.treasurehunt.gui.TreasureCompletedGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TreasureCommand implements CommandExecutor, TabCompleter {
    private final TreasureHuntPlugin plugin;
    private final TreasureManager treasureManager;

    public TreasureCommand(TreasureHuntPlugin plugin, TreasureManager treasureManager) {
        this.plugin = plugin;
        this.treasureManager = treasureManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("treasurehunt.admin")) {
            String noPermMsg = plugin.getConfig().getString("messages.no-permission", "&c&l! &cYou don't have permission to use this command.");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', noPermMsg));
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                handleCreateCommand(player, args);
                break;
            case "delete":
                handleDeleteCommand(player, args);
                break;
            case "completed":
                handleCompletedCommand(player, args);
                break;
            case "list":
                handleListCommand(player);
                break;
            case "gui":
                handleGuiCommand(player);
                break;
            case "help":
                sendHelpMessage(player);
                break;
            default:
                String invalidMsg = plugin.getConfig().getString("messages.invalid-command", "&c&l! &cInvalid command syntax. Type &e/treasure help &cfor assistance.");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', invalidMsg));
        }

        return true;
    }

    private void handleCreateCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/treasure create <id> <command>");
            player.sendMessage(ChatColor.GRAY + "Example: " + ChatColor.WHITE + "/treasure create diamond_chest say %player% found a treasure!");
            return;
        }

        String treasureId = args[1];
        StringBuilder commandBuilder = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            commandBuilder.append(args[i]);
            if (i < args.length - 1) {
                commandBuilder.append(" ");
            }
        }
        String rewardCommand = commandBuilder.toString();

        treasureManager.startTreasureCreation(player, treasureId, rewardCommand);
    }

    private void handleDeleteCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/treasure delete <id>");
            return;
        }

        String treasureId = args[1];
        treasureManager.deleteTreasure(treasureId).thenAccept(success -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (success) {
                    String deletedMsg = plugin.getConfig().getString("messages.treasure-deleted", "&c&l✗ &cTreasure &e&l%id% &chas been deleted from the database.");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', deletedMsg.replace("%id%", treasureId)));
                } else {
                    String notFoundMsg = plugin.getConfig().getString("messages.treasure-not-found", "&c&l! &cCouldn't find treasure &e&l%id% &cin the database.");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', notFoundMsg.replace("%id%", treasureId)));
                }
            });
        });
    }

    private void handleCompletedCommand(Player player, String[] args) {
        if (args.length > 1) {
            player.sendMessage(ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/treasure completed");
            player.sendMessage(ChatColor.GRAY + "This will show all treasures - click one to see who found it");
            return;
        }

        new TreasureCompletedGUI(plugin, treasureManager).openGUI(player);
    }

    private void handleListCommand(Player player) {
        treasureManager.getTreasureList().thenAccept(treasures -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (treasures.isEmpty()) {
                    String noTreasuresMsg = plugin.getConfig().getString("messages.no-treasures", "&e&l! &eThere are no treasures in the database yet.");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', noTreasuresMsg));
                } else {
                    player.sendMessage(ChatColor.GREEN + "Active Treasures (" + ChatColor.WHITE + treasures.size() + ChatColor.GREEN + "):");
                    for (String treasure : treasures) {
                        player.sendMessage(ChatColor.GRAY + "  • " + treasure);
                    }
                }
            });
        });
    }

    private void handleGuiCommand(Player player) {
        new TreasureGUI(plugin, treasureManager).openGUI(player);
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "╔══════════════════════════════════╗");
        player.sendMessage(ChatColor.GOLD + "║        " + ChatColor.YELLOW + "Treasure Hunt Commands" + ChatColor.GOLD + "        ║");
        player.sendMessage(ChatColor.GOLD + "╠══════════════════════════════════╣");
        player.sendMessage(ChatColor.YELLOW + "║ " + ChatColor.WHITE + "/treasure create <id> <command>" + ChatColor.GRAY + " - Create treasure");
        player.sendMessage(ChatColor.YELLOW + "║ " + ChatColor.WHITE + "/treasure delete <id>" + ChatColor.GRAY + " - Remove treasure");
        player.sendMessage(ChatColor.YELLOW + "║ " + ChatColor.WHITE + "/treasure completed" + ChatColor.GRAY + " - View treasure finders");
        player.sendMessage(ChatColor.YELLOW + "║ " + ChatColor.WHITE + "/treasure list" + ChatColor.GRAY + " - Show all treasures");
        player.sendMessage(ChatColor.YELLOW + "║ " + ChatColor.WHITE + "/treasure gui" + ChatColor.GRAY + " - Open management GUI");
        player.sendMessage(ChatColor.YELLOW + "║ " + ChatColor.WHITE + "/treasure help" + ChatColor.GRAY + " - Show this help");
        player.sendMessage(ChatColor.GOLD + "╚══════════════════════════════════╝");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("treasurehunt.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("create", "delete", "completed", "list", "gui", "help");
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("delete") || subCommand.equals("completed")) {
                return new ArrayList<>();
            }
        }

        return new ArrayList<>();
    }
}
