package com.treasurehunt.models;

import org.bukkit.Location;

public class Treasure {
    private final String id;
    private final Location location;
    private final String command;

    public Treasure(String id, Location location, String command) {
        this.id = id;
        this.location = location;
        this.command = command;
    }

    public String getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public String getCommand() {
        return command;
    }

    public boolean isAtLocation(Location location) {
        if (location == null || this.location == null) {
            return false;
        }
        
        return this.location.getWorld().getName().equals(location.getWorld().getName()) &&
               this.location.getBlockX() == location.getBlockX() &&
               this.location.getBlockY() == location.getBlockY() &&
               this.location.getBlockZ() == location.getBlockZ();
    }

    @Override
    public String toString() {
        return String.format("Treasure{id='%s', location=%s, command='%s'}", 
            id, location, command);
    }
}
