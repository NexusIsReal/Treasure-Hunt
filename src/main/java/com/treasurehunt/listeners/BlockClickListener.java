package com.treasurehunt.listeners;

import com.treasurehunt.TreasureHuntPlugin;
import com.treasurehunt.managers.TreasureManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class BlockClickListener implements Listener {
    private final TreasureManager treasureManager;

    public BlockClickListener(TreasureManager treasureManager) {
        this.treasureManager = treasureManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        
        if (!player.hasPermission("treasurehunt.use")) {
            return;
        }

        if (treasureManager.isPlayerCreatingTreasure(player.getUniqueId())) {
            event.setCancelled(true);
            treasureManager.handleBlockSelection(player, event.getClickedBlock().getLocation());
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            treasureManager.claimTreasure(player, event.getClickedBlock().getLocation())
                .thenAccept(claimedSuccessfully -> {
                    if (claimedSuccessfully) {
                        TreasureHuntPlugin plugin = treasureManager.getPlugin();
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            String claimedMsg = plugin.getConfig().getString("messages.treasure-claimed", "&a&l★ &aTreasure claimed! The reward command has been executed.");
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', claimedMsg));
                        });
                    }
                });
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        if (treasureManager.isTreasureAtLocation(event.getBlock().getLocation())) {
            event.setCancelled(true);
            TreasureHuntPlugin plugin = treasureManager.getPlugin();
            String unbreakableMsg = plugin.getConfig().getString("messages.treasure-unbreakable", "&c&l! &cYou cannot break treasure blocks!");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', unbreakableMsg));
        }
    }
}
