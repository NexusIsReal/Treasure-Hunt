package com.treasurehunt;

import com.treasurehunt.commands.TreasureCommand;
import com.treasurehunt.database.DatabaseManager;
import com.treasurehunt.listeners.BlockClickListener;
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
        
        getCommand("treasure").setExecutor(new TreasureCommand(this, treasureManager));
        
        getServer().getPluginManager().registerEvents(new BlockClickListener(treasureManager), this);
        
        getLogger().info("Treasure Hunt plugin has been enabled successfully!");
        getLogger().info("Use /treasure help to see available commands");
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down Treasure Hunt plugin...");
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
