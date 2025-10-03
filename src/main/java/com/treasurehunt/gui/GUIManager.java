package com.treasurehunt.gui;

import com.treasurehunt.TreasureHuntPlugin;
import com.treasurehunt.managers.TreasureManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GUIManager implements Listener {
    private final TreasureHuntPlugin plugin;
    private final TreasureManager treasureManager;

    public GUIManager(TreasureHuntPlugin plugin, TreasureManager treasureManager) {
        this.plugin = plugin;
        this.treasureManager = treasureManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        if (title.equals(ChatColor.DARK_GREEN + "Treasure Management")) {
            handleMainGUI(event, player, clickedItem);
        } else if (title.equals(ChatColor.DARK_RED + "Confirm Deletion")) {
            handleConfirmationGUI(event, player, clickedItem);
        } else if (title.startsWith(ChatColor.DARK_GREEN + "Treasure Completion Tracker") || 
                   title.startsWith(ChatColor.DARK_GREEN + "Treasure: ")) {
            handleCompletedGUI(event, player, clickedItem, title);
        }
    }

    private void handleMainGUI(InventoryClickEvent event, Player player, ItemStack clickedItem) {
        event.setCancelled(true);

        if (clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        if (clickedItem.getType() == Material.EMERALD) {
            new TreasureGUI(plugin, treasureManager).openGUI(player);
            player.sendMessage(ChatColor.GREEN + "✓ Treasure list refreshed!");
            return;
        }

        if (clickedItem.getType() == Material.CHEST) {
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String treasureName = ChatColor.stripColor(meta.getDisplayName());
                String treasureId = treasureName.split(" ")[0];
                
                player.closeInventory();
                new ConfirmationGUI(plugin, treasureManager, treasureId, treasureName).openGUI(player);
            }
        }
    }

    private void handleConfirmationGUI(InventoryClickEvent event, Player player, ItemStack clickedItem) {
        event.setCancelled(true);

        if (clickedItem.getType() == Material.RED_CONCRETE) {
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.hasLore()) {
                List<String> lore = meta.getLore();
                if (lore.size() > 0) {
                    String treasureInfo = lore.get(0);
                    String treasureName = treasureInfo.replace(ChatColor.GRAY + "Treasure: " + ChatColor.YELLOW, "");
                    String treasureId = treasureName.split(" ")[0];
                    
                    player.closeInventory();
                    treasureManager.deleteTreasure(treasureId).thenAccept(deletedSuccessfully -> {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (deletedSuccessfully) {
                                player.sendMessage(ChatColor.GREEN + "✓ Treasure " + ChatColor.WHITE + treasureId + ChatColor.GREEN + " has been deleted successfully!");
                            } else {
                                player.sendMessage(ChatColor.RED + "✗ Failed to delete treasure " + ChatColor.WHITE + treasureId + ChatColor.RED + " from the database!");
                            }
                        });
                    });
                }
            }
            return;
        }

        if (clickedItem.getType() == Material.GREEN_CONCRETE) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Treasure deletion cancelled.");
            return;
        }
    }

    private void handleCompletedGUI(InventoryClickEvent event, Player player, ItemStack clickedItem, String title) {
        event.setCancelled(true);

        if (clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        if (clickedItem.getType() == Material.ARROW) {
            if (title.startsWith(ChatColor.DARK_GREEN + "Treasure: ")) {
                new TreasureCompletedGUI(plugin, treasureManager).openGUI(player);
            } else {
                player.closeInventory();
                new TreasureGUI(plugin, treasureManager).openGUI(player);
            }
            return;
        }

        if (clickedItem.getType() == Material.CHEST && title.equals(ChatColor.DARK_GREEN + "Treasure Completion Tracker")) {
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String treasureName = ChatColor.stripColor(meta.getDisplayName());
                String treasureId = treasureName.split(" ")[0];
                
                new TreasureCompletedGUI(plugin, treasureManager, treasureId).openGUI(player);
            }
            return;
        }
    }
}
