package com.treasurehunt.listeners;

import com.treasurehunt.managers.TreasureManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerDisconnectListener implements Listener {
    private final TreasureManager treasureManager;

    public PlayerDisconnectListener(TreasureManager treasureManager) {
        this.treasureManager = treasureManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        treasureManager.cancelTreasureCreation(event.getPlayer().getUniqueId());
    }
}
