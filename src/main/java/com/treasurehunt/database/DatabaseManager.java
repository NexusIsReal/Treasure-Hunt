package com.treasurehunt.database;

import com.treasurehunt.models.Treasure;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {
    private HikariDataSource dataSource;
    private final FileConfiguration config;
    private volatile boolean isConnected = false;

    public DatabaseManager(FileConfiguration config) {
        this.config = config;
    }

    public void initialize() {
        try {
            HikariConfig hikariConfig = new HikariConfig();
            
            String dbHost = config.getString("database.host", "localhost");
            int dbPort = config.getInt("database.port", 3306);
            String dbName = config.getString("database.database", "treasurehunt");
            String dbUser = config.getString("database.username", "root");
            String dbPass = config.getString("database.password", "password");
            boolean useSSL = config.getBoolean("database.ssl", false);
            
            String connectionUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=%s&serverTimezone=UTC", 
                dbHost, dbPort, dbName, useSSL);
            
            hikariConfig.setJdbcUrl(connectionUrl);
            hikariConfig.setUsername(dbUser);
            hikariConfig.setPassword(dbPass);
            hikariConfig.setMaximumPoolSize(config.getInt("database.maximum-pool-size", 10));
            hikariConfig.setMinimumIdle(config.getInt("database.minimum-idle", 5));
            hikariConfig.setConnectionTimeout(config.getLong("database.connection-timeout", 30000));
            hikariConfig.setLeakDetectionThreshold(30000);
            hikariConfig.setIdleTimeout(600000);
            hikariConfig.setMaxLifetime(1800000);
            hikariConfig.setValidationTimeout(5000);
            
            dataSource = new HikariDataSource(hikariConfig);
            
            testConnection();
            createTables();
            
            isConnected = true;
            Bukkit.getLogger().info("Database connection established successfully");
        } catch (Exception e) {
            Bukkit.getLogger().severe("Failed to initialize database connection: " + e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private void testConnection() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {
                Bukkit.getLogger().info("Database connection test successful");
            } else {
                throw new SQLException("Database connection is not valid");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database connection test failed", e);
        }
    }

    private void createTables() {
        try (Connection connection = dataSource.getConnection()) {
            String treasuresTableSQL = """
                CREATE TABLE IF NOT EXISTS treasures (
                    id VARCHAR(255) PRIMARY KEY,
                    world VARCHAR(255) NOT NULL,
                    x INT NOT NULL,
                    y INT NOT NULL,
                    z INT NOT NULL,
                    command TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
            
            String foundTableSQL = """
                CREATE TABLE IF NOT EXISTS treasure_found (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    treasure_id VARCHAR(255) NOT NULL,
                    player_uuid VARCHAR(36) NOT NULL,
                    found_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (treasure_id) REFERENCES treasures(id) ON DELETE CASCADE,
                    UNIQUE KEY unique_treasure_player (treasure_id, player_uuid)
                )
                """;
            
            try (PreparedStatement treasuresStmt = connection.prepareStatement(treasuresTableSQL);
                 PreparedStatement foundStmt = connection.prepareStatement(foundTableSQL)) {
                treasuresStmt.execute();
                foundStmt.execute();
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Failed to create database tables: " + e.getMessage());
        }
    }

    public CompletableFuture<Boolean> createTreasure(String treasureId, String worldName, int x, int y, int z, String rewardCommand) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                String insertSQL = "INSERT INTO treasures (id, world, x, y, z, command) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = connection.prepareStatement(insertSQL)) {
                    stmt.setString(1, treasureId);
                    stmt.setString(2, worldName);
                    stmt.setInt(3, x);
                    stmt.setInt(4, y);
                    stmt.setInt(5, z);
                    stmt.setString(6, rewardCommand);
                    return stmt.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                Bukkit.getLogger().severe("Failed to create treasure: " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> deleteTreasure(String treasureId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                String deleteSQL = "DELETE FROM treasures WHERE id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(deleteSQL)) {
                    stmt.setString(1, treasureId);
                    return stmt.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                Bukkit.getLogger().severe("Failed to delete treasure: " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<List<String>> getTreasureList() {
        return CompletableFuture.supplyAsync(() -> {
            List<String> treasureList = new ArrayList<>();
            try (Connection connection = dataSource.getConnection()) {
                String selectSQL = "SELECT id, world, x, y, z FROM treasures ORDER BY id";
                try (PreparedStatement stmt = connection.prepareStatement(selectSQL);
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String treasureId = rs.getString("id");
                        String worldName = rs.getString("world");
                        int x = rs.getInt("x");
                        int y = rs.getInt("y");
                        int z = rs.getInt("z");
                        treasureList.add(String.format("%s (%s: %d,%d,%d)", treasureId, worldName, x, y, z));
                    }
                }
            } catch (SQLException e) {
                Bukkit.getLogger().severe("Failed to get treasure list: " + e.getMessage());
            }
            return treasureList;
        });
    }

    public CompletableFuture<List<String>> getTreasureCompleted(String treasureId) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> foundPlayers = new ArrayList<>();
            try (Connection connection = dataSource.getConnection()) {
                String selectSQL = """
                    SELECT p.player_uuid, p.found_at 
                    FROM treasure_found p 
                    WHERE p.treasure_id = ? 
                    ORDER BY p.found_at
                    """;
                try (PreparedStatement stmt = connection.prepareStatement(selectSQL)) {
                    stmt.setString(1, treasureId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            String playerUuid = rs.getString("player_uuid");
                            String foundAt = rs.getString("found_at");
                            foundPlayers.add(String.format("%s - %s", playerUuid, foundAt));
                        }
                    }
                }
            } catch (SQLException e) {
                Bukkit.getLogger().severe("Failed to get treasure completed: " + e.getMessage());
            }
            return foundPlayers;
        });
    }

    public CompletableFuture<Boolean> hasPlayerFoundTreasure(String treasureId, UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                String checkSQL = "SELECT 1 FROM treasure_found WHERE treasure_id = ? AND player_uuid = ?";
                try (PreparedStatement stmt = connection.prepareStatement(checkSQL)) {
                    stmt.setString(1, treasureId);
                    stmt.setString(2, playerUuid.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        return rs.next();
                    }
                }
            } catch (SQLException e) {
                Bukkit.getLogger().severe("Failed to check if player found treasure: " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> markTreasureFound(String treasureId, UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                String insertSQL = "INSERT INTO treasure_found (treasure_id, player_uuid) VALUES (?, ?)";
                try (PreparedStatement stmt = connection.prepareStatement(insertSQL)) {
                    stmt.setString(1, treasureId);
                    stmt.setString(2, playerUuid.toString());
                    return stmt.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                Bukkit.getLogger().severe("Failed to mark treasure as found: " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<String> getTreasureCommand(String treasureId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                String selectSQL = "SELECT command FROM treasures WHERE id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(selectSQL)) {
                    stmt.setString(1, treasureId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getString("command");
                        }
                    }
                }
            } catch (SQLException e) {
                Bukkit.getLogger().severe("Failed to get treasure command: " + e.getMessage());
            }
            return null;
        });
    }

    public CompletableFuture<Map<String, Treasure>> loadAllTreasures() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Treasure> loadedTreasures = new HashMap<>();
            try (Connection connection = dataSource.getConnection()) {
                String selectSQL = "SELECT id, world, x, y, z, command FROM treasures";
                try (PreparedStatement stmt = connection.prepareStatement(selectSQL);
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String treasureId = rs.getString("id");
                        String worldName = rs.getString("world");
                        int x = rs.getInt("x");
                        int y = rs.getInt("y");
                        int z = rs.getInt("z");
                        String rewardCommand = rs.getString("command");
                        
                        org.bukkit.World world = Bukkit.getWorld(worldName);
                        if (world != null) {
                            Location location = new Location(world, x, y, z);
                            Treasure treasure = new Treasure(treasureId, location, rewardCommand);
                            loadedTreasures.put(treasureId, treasure);
                        } else {
                            Bukkit.getLogger().warning("World '" + worldName + "' not found for treasure '" + treasureId + "'");
                        }
                    }
                }
            } catch (SQLException e) {
                Bukkit.getLogger().severe("Failed to load treasures from database: " + e.getMessage());
            }
            return loadedTreasures;
        });
    }

    public boolean isConnected() {
        return isConnected && dataSource != null && !dataSource.isClosed();
    }

    public void close() {
        isConnected = false;
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            Bukkit.getLogger().info("Database connection closed");
        }
    }
}
