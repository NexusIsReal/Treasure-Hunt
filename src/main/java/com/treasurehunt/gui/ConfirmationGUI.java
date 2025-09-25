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

public class ConfirmationGUI implements Listener {
    private final TreasureHuntPlugin plugin;
    private final TreasureManager treasureManager;
    private final Inventory inventory;
    private final String treasureId;
    private final String treasureName;

    public ConfirmationGUI(TreasureHuntPlugin plugin, TreasureManager treasureManager, String treasureId, String treasureName) {
        this.plugin = plugin;
        this.treasureManager = treasureManager;
        this.treasureId = treasureId;
        this.treasureName = treasureName;
        this.inventory = Bukkit.createInventory(null, 27, ChatColor.DARK_RED + "Confirm Deletion");
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        setupGUI();
    }

    private void setupGUI() {
        ItemStack confirmItem = new ItemStack(Material.RED_CONCRETE);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.RED + "DELETE TREASURE");
        
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add(ChatColor.GRAY + "Treasure: " + ChatColor.YELLOW + treasureName);
        confirmLore.add(ChatColor.RED + "This action cannot be undone!");
        confirmLore.add("");
        confirmLore.add(ChatColor.RED + "Click to confirm deletion");
        confirmMeta.setLore(confirmLore);
        confirmItem.setItemMeta(confirmMeta);
        inventory.setItem(11, confirmItem);

        ItemStack cancelItem = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.GREEN + "CANCEL");
        
        List<String> cancelLore = new ArrayList<>();
        cancelLore.add(ChatColor.GRAY + "Treasure: " + ChatColor.YELLOW + treasureName);
        cancelLore.add(ChatColor.GREEN + "Click to cancel deletion");
        cancelMeta.setLore(cancelLore);
        cancelItem.setItemMeta(cancelMeta);
        inventory.setItem(15, cancelItem);

        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName(ChatColor.YELLOW + "Delete Treasure Confirmation");
        
        List<String> infoLore = new ArrayList<>();
        infoLore.add(ChatColor.GRAY + "Are you sure you want to delete");
        infoLore.add(ChatColor.YELLOW + treasureName + "?");
        infoLore.add("");
        infoLore.add(ChatColor.RED + "This will permanently remove");
        infoLore.add(ChatColor.RED + "the treasure and all associated data!");
        infoMeta.setLore(infoLore);
        infoItem.setItemMeta(infoMeta);
        inventory.setItem(13, infoItem);
    }

    public void openGUI(Player player) {
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        if (clickedItem.getType() == Material.RED_CONCRETE) {
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
            return;
        }

        if (clickedItem.getType() == Material.GREEN_CONCRETE) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Treasure deletion cancelled.");
            return;
        }
    }
}
