package com.treasurehunt.managers;

import com.treasurehunt.TreasureHuntPlugin;
import com.treasurehunt.database.DatabaseManager;
import com.treasurehunt.models.Treasure;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TreasureManager {
    private final TreasureHuntPlugin plugin;
    private final DatabaseManager databaseManager;
    private final Map<String, Treasure> treasures;
    private final Map<UUID, String> pendingCreations;

    public TreasureManager(TreasureHuntPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.treasures = new HashMap<>();
        this.pendingCreations = new HashMap<>();
        loadTreasuresFromDatabase();
    }

    public void startTreasureCreation(Player player, String treasureId, String rewardCommand) {
        pendingCreations.put(player.getUniqueId(), treasureId + ":" + rewardCommand);
        String selectionMsg = plugin.getConfig().getString("messages.block-selection", "&a&l→ &aClick on any block to set it as treasure &e&l%id%");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', selectionMsg.replace("%id%", treasureId)));
    }

    public void handleBlockSelection(Player player, Location location) {
        UUID playerUuid = player.getUniqueId();
        if (!pendingCreations.containsKey(playerUuid)) {
            return;
        }

        String[] creationData = pendingCreations.get(playerUuid).split(":", 2);
        String treasureId = creationData[0];
        String rewardCommand = creationData[1];

        pendingCreations.remove(playerUuid);

        Treasure newTreasure = new Treasure(treasureId, location, rewardCommand);
        treasures.put(treasureId, newTreasure);

        databaseManager.createTreasure(treasureId, location.getWorld().getName(), 
            location.getBlockX(), location.getBlockY(), location.getBlockZ(), rewardCommand)
            .thenAccept(success -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (success) {
                        String successMsg = plugin.getConfig().getString("messages.block-selected", "&a&l✓ &aBlock selected! Treasure &e&l%id% &ais now active and ready to be found.");
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', successMsg.replace("%id%", treasureId)));
                    } else {
                        String failMsg = plugin.getConfig().getString("messages.treasure-creation-failed", "&c&l✗ &cFailed to create treasure in the database. Check your connection.");
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', failMsg));
                    }
                });
            });
    }

    public CompletableFuture<Boolean> deleteTreasure(String treasureId) {
        treasures.remove(treasureId);
        return databaseManager.deleteTreasure(treasureId);
    }

    public CompletableFuture<Boolean> hasPlayerFoundTreasure(String treasureId, UUID playerUuid) {
        return databaseManager.hasPlayerFoundTreasure(treasureId, playerUuid);
    }

    public CompletableFuture<Boolean> claimTreasure(Player player, Location location) {
        Treasure treasure = findTreasureAtLocation(location);
        if (treasure == null) {
            return CompletableFuture.completedFuture(false);
        }

        return hasPlayerFoundTreasure(treasure.getId(), player.getUniqueId())
            .thenCompose(alreadyFound -> {
                if (alreadyFound) {
                    return CompletableFuture.completedFuture(false);
                }

                return databaseManager.markTreasureFound(treasure.getId(), player.getUniqueId())
                    .thenCompose(markedSuccessfully -> {
                        if (markedSuccessfully) {
                            return databaseManager.getTreasureCommand(treasure.getId())
                                .thenApply(rewardCommand -> {
                                    if (rewardCommand != null) {
                                        String finalCommand = rewardCommand.replace("%player%", player.getName());
                                        Bukkit.getScheduler().runTask(plugin, () -> {
                                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                                        });
                                        return true;
                                    }
                                    return false;
                                });
                        }
                        return CompletableFuture.completedFuture(false);
                    });
            });
    }

    private Treasure findTreasureAtLocation(Location location) {
        for (Treasure treasure : treasures.values()) {
            if (treasure.isAtLocation(location)) {
                return treasure;
            }
        }
        return null;
    }

    public boolean isTreasureAtLocation(Location location) {
        return findTreasureAtLocation(location) != null;
    }

    public CompletableFuture<java.util.List<String>> getTreasureList() {
        return databaseManager.getTreasureList();
    }

    public CompletableFuture<java.util.List<String>> getTreasureCompleted(String id) {
        return databaseManager.getTreasureCompleted(id);
    }

    public boolean isPlayerCreatingTreasure(UUID playerUuid) {
        return pendingCreations.containsKey(playerUuid);
    }

    public void cancelTreasureCreation(UUID playerUuid) {
        pendingCreations.remove(playerUuid);
    }

    public TreasureHuntPlugin getPlugin() {
        return plugin;
    }

    private void loadTreasuresFromDatabase() {
        databaseManager.loadAllTreasures().thenAccept(loadedTreasures -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                treasures.putAll(loadedTreasures);
                if (loadedTreasures.size() > 0) {
                    plugin.getLogger().info("Successfully loaded " + loadedTreasures.size() + " treasures from the database");
                } else {
                    plugin.getLogger().info("No treasures found in the database");
                }
            });
        });
    }
}
