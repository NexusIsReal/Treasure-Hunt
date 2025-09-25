# Treasure Hunt Plugin

A Minecraft Paper plugin that allows administrators to create custom treasures across the world. Players can find these treasures by clicking on blocks, and each treasure can only be claimed once per player.

## Features

- **Multi-server Support**: Uses MySQL database for cross-server treasure claiming
- **Custom Commands**: Execute any command when a treasure is found
- **Player Placeholders**: Use `%player%` in commands to reference the finder
- **GUI Management**: Visual interface for managing treasures
- **One-time Claiming**: Each player can only claim each treasure once
- **Block Selection**: Interactive block selection for treasure placement

## Commands

- `/treasure create <id> <command>` - Create a new treasure (requires block selection)
- `/treasure delete <id>` - Delete a treasure
- `/treasure completed <id>` - Show who found a specific treasure
- `/treasure list` - List all treasures
- `/treasure gui` - Open the treasure management GUI
- `/treasure help` - Show help message

## Permissions

- `treasurehunt.admin` - Access to all treasure commands (default: op)
- `treasurehunt.use` - Ability to find treasures (default: true)

## Installation

1. Build the plugin using Gradle:
   ```bash
   gradle build jar
   ```

2. Place the generated JAR file in your server's `plugins` folder

3. Configure the database settings in `config.yml`

4. Restart your server

## Configuration

Edit `config.yml` to configure your MySQL database:

```yaml
database:
  host: localhost
  port: 3306
  database: treasurehunt
  username: root
  password: password
  ssl: false
  connection-timeout: 30000
  maximum-pool-size: 10
  minimum-idle: 5
```

## Usage

### Creating a Treasure

1. Use `/treasure create example say %player% found a treasure!`
2. Click on any block to set it as the treasure location
3. The treasure is now active and can be found by players

### Finding Treasures

Players simply right-click on treasure blocks to claim them. The configured command will be executed from the console with `%player%` replaced by the player's name.

### Multi-Server Setup

The plugin automatically handles multi-server environments. When a player claims a treasure on one server, it's marked as claimed across all servers using the same database.

## Requirements

- Paper 1.21.4 or higher
- MySQL database
- Java 21

## Building

```bash
./gradlew clean jar
```

The compiled JAR will be in `build/libs/`.
