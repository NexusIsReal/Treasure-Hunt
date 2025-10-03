package com.treasurehunt.gui;

import com.treasurehunt.TreasureHuntPlugin;
import com.treasurehunt.managers.TreasureManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TreasureCompletedGUI implements Listener {
    private final TreasureHuntPlugin plugin;
    private final TreasureManager treasureManager;
    private final Inventory inventory;
    private String selectedTreasureId;

    public TreasureCompletedGUI(TreasureHuntPlugin plugin, TreasureManager treasureManager) {
        this.plugin = plugin;
        this.treasureManager = treasureManager;
        this.inventory = Bukkit.createInventory(null, 54, ChatColor.DARK_GREEN + "Treasure Completion Tracker");
        
        loadAllTreasures();
    }

    public TreasureCompletedGUI(TreasureHuntPlugin plugin, TreasureManager treasureManager, String treasureId) {
        this.plugin = plugin;
        this.treasureManager = treasureManager;
        this.selectedTreasureId = treasureId;
        this.inventory = Bukkit.createInventory(null, 54, ChatColor.DARK_GREEN + "Treasure: " + treasureId);
        
        loadCompletedPlayers();
    }

    private void loadAllTreasures() {
        inventory.clear();
        
        treasureManager.getTreasureList().thenAccept(treasures -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (treasures.isEmpty()) {
                    ItemStack noTreasuresItem = new ItemStack(Material.BARRIER);
                    ItemMeta noTreasuresMeta = noTreasuresItem.getItemMeta();
                    noTreasuresMeta.setDisplayName(ChatColor.RED + "No Treasures Found");
                    
                    List<String> noTreasuresLore = new ArrayList<>();
                    noTreasuresLore.add(ChatColor.GRAY + "There are no treasures in the database yet");
                    noTreasuresMeta.setLore(noTreasuresLore);
                    noTreasuresItem.setItemMeta(noTreasuresMeta);
                    inventory.setItem(22, noTreasuresItem);
                } else {
                    int slot = 0;
                    for (String treasureInfo : treasures) {
                        if (slot >= 45) break;
                        
                        String treasureId = treasureInfo.split(" ")[0];
                        
                        ItemStack treasureItem = new ItemStack(Material.CHEST);
                        ItemMeta treasureMeta = treasureItem.getItemMeta();
                        treasureMeta.setDisplayName(ChatColor.GOLD + treasureInfo);
                        
                        List<String> treasureLore = new ArrayList<>();
                        treasureLore.add(ChatColor.GRAY + "Click to see who found this treasure");
                        treasureMeta.setLore(treasureLore);
                        
                        treasureItem.setItemMeta(treasureMeta);
                        inventory.setItem(slot, treasureItem);
                        slot++;
                    }
                }
                
                ItemStack backItem = new ItemStack(Material.ARROW);
                ItemMeta backMeta = backItem.getItemMeta();
                backMeta.setDisplayName(ChatColor.YELLOW + "Back to Main Menu");
                backItem.setItemMeta(backMeta);
                inventory.setItem(49, backItem);
                
                ItemStack closeItem = new ItemStack(Material.BARRIER);
                ItemMeta closeMeta = closeItem.getItemMeta();
                closeMeta.setDisplayName(ChatColor.RED + "Close");
                closeItem.setItemMeta(closeMeta);
                inventory.setItem(53, closeItem);
            });
        });
    }

    private void loadCompletedPlayers() {
        inventory.clear();
        
        treasureManager.getTreasureCompleted(selectedTreasureId).thenAccept(players -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (players.isEmpty()) {
                    ItemStack noPlayersItem = new ItemStack(Material.BARRIER);
                    ItemMeta noPlayersMeta = noPlayersItem.getItemMeta();
                    noPlayersMeta.setDisplayName(ChatColor.RED + "No Players Found This Treasure");
                    
                    List<String> noPlayersLore = new ArrayList<>();
                    noPlayersLore.add(ChatColor.GRAY + "Treasure: " + ChatColor.YELLOW + selectedTreasureId);
                    noPlayersLore.add(ChatColor.GRAY + "No one has discovered this treasure yet");
                    noPlayersMeta.setLore(noPlayersLore);
                    noPlayersItem.setItemMeta(noPlayersMeta);
                    inventory.setItem(22, noPlayersItem);
                } else {
                    int slot = 0;
                    for (String playerInfo : players) {
                        if (slot >= 45) break;
                        
                        String[] parts = playerInfo.split(" - ");
                        if (parts.length >= 2) {
                            String uuidString = parts[0];
                            String foundAt = parts[1];
                            
                            try {
                                UUID playerUuid = UUID.fromString(uuidString);
                                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
                                String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown Player";
                                
                                ItemStack playerItem = new ItemStack(Material.PLAYER_HEAD);
                                ItemMeta playerMeta = playerItem.getItemMeta();
                                playerMeta.setDisplayName(ChatColor.GREEN + playerName);
                                
                                List<String> playerLore = new ArrayList<>();
                                playerLore.add(ChatColor.GRAY + "Found: " + ChatColor.YELLOW + foundAt);
                                playerLore.add(ChatColor.GRAY + "Treasure: " + ChatColor.YELLOW + selectedTreasureId);
                                playerMeta.setLore(playerLore);
                                
                                playerItem.setItemMeta(playerMeta);
                                inventory.setItem(slot, playerItem);
                                slot++;
                            } catch (IllegalArgumentException e) {
                                continue;
                            }
                        }
                    }
                }
                
                ItemStack backItem = new ItemStack(Material.ARROW);
                ItemMeta backMeta = backItem.getItemMeta();
                backMeta.setDisplayName(ChatColor.YELLOW + "Back to Main Menu");
                backItem.setItemMeta(backMeta);
                inventory.setItem(49, backItem);
                
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

        if (clickedItem.getType() == Material.ARROW) {
            if (selectedTreasureId != null) {
                new TreasureCompletedGUI(plugin, treasureManager).openGUI(player);
            } else {
                player.closeInventory();
                new TreasureGUI(plugin, treasureManager).openGUI(player);
            }
            return;
        }

        if (clickedItem.getType() == Material.CHEST && selectedTreasureId == null) {
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
