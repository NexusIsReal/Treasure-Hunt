package com.treasurehunt;

import com.treasurehunt.commands.TreasureCommand;
import com.treasurehunt.database.DatabaseManager;
import com.treasurehunt.gui.GUIManager;
import com.treasurehunt.listeners.BlockClickListener;
import com.treasurehunt.listeners.PlayerDisconnectListener;
import com.treasurehunt.managers.TreasureManager;
import org.bukkit.plugin.java.JavaPlugin;

public class TreasureHuntPlugin extends JavaPlugin {
    private DatabaseManager databaseManager;
    private TreasureManager treasureManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        getLogger().info("Initializing Treasure Hunt plugin...");
        
        databaseManager = new DatabaseManager(getConfig());
        databaseManager.initialize();
        
        treasureManager = new TreasureManager(this, databaseManager);
        
        TreasureCommand treasureCommand = new TreasureCommand(this, treasureManager);
        getCommand("treasure").setExecutor(treasureCommand);
        getCommand("treasure").setTabCompleter(treasureCommand);
        
        getServer().getPluginManager().registerEvents(new BlockClickListener(treasureManager), this);
        getServer().getPluginManager().registerEvents(new PlayerDisconnectListener(treasureManager), this);
        new GUIManager(this, treasureManager);
        
        getLogger().info("Treasure Hunt plugin has been enabled successfully!");
        getLogger().info("Use /treasure help to see available commands");
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down Treasure Hunt plugin...");
        if (treasureManager != null) {
            treasureManager.cleanup();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("Treasure Hunt plugin has been disabled successfully!");
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public TreasureManager getTreasureManager() {
        return treasureManager;
    }
}
