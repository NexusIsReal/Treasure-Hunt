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

public class TreasureGUI implements Listener {
    private final TreasureHuntPlugin plugin;
    private final TreasureManager treasureManager;
    private final Inventory inventory;

    public TreasureGUI(TreasureHuntPlugin plugin, TreasureManager treasureManager) {
        this.plugin = plugin;
        this.treasureManager = treasureManager;
        this.inventory = Bukkit.createInventory(null, 54, ChatColor.DARK_GREEN + "Treasure Management");
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadTreasures();
    }

    private void loadTreasures() {
        inventory.clear();
        
        treasureManager.getTreasureList().thenAccept(treasures -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                int slot = 0;
                for (String treasureInfo : treasures) {
                    if (slot >= 45) break;
                    
                    ItemStack item = new ItemStack(Material.CHEST);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName(ChatColor.GOLD + treasureInfo);
                    
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + "Click to delete this treasure");
                    lore.add(ChatColor.YELLOW + "You will be asked to confirm");
                    meta.setLore(lore);
                    
                    item.setItemMeta(meta);
                    inventory.setItem(slot, item);
                    slot++;
                }
                
                ItemStack refreshItem = new ItemStack(Material.EMERALD);
                ItemMeta refreshMeta = refreshItem.getItemMeta();
                refreshMeta.setDisplayName(ChatColor.GREEN + "Refresh List");
                refreshItem.setItemMeta(refreshMeta);
                inventory.setItem(49, refreshItem);
                
                ItemStack closeItem = new ItemStack(Material.BARRIER);
                ItemMeta closeMeta = closeItem.getItemMeta();
                closeMeta.setDisplayName(ChatColor.RED + "Close");
                closeItem.setItemMeta(closeMeta);
                inventory.setItem(53, closeItem);
            });
        });
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

        if (clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        if (clickedItem.getType() == Material.EMERALD) {
            loadTreasures();
            player.sendMessage(ChatColor.GREEN + "✓ Treasure list refreshed!");
            return;
        }

        if (clickedItem.getType() == Material.CHEST) {
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String treasureName = ChatColor.stripColor(meta.getDisplayName());
                String treasureId = treasureName.split(" ")[0];
                
                player.closeInventory();
                ConfirmationGUI confirmationGUI = new ConfirmationGUI(plugin, treasureManager, treasureId, treasureName);
                confirmationGUI.openGUI(player);
            }
        }
    }
}
