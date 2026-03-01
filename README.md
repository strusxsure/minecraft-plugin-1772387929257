# Fire Mace Plugin

A Minecraft Spigot plugin that adds a custom Fire Mace weapon with area-based effects.

## Features

- **Fire Mace**: Custom weapon with special abilities
- **Heat Damage Over Time**: Area-based damage to nearby entities
- **Lava Trap Triggers**: Creates lava traps on interaction or activation
- **Fire Mob Spawn Zones**: Spawns blaze mobs around the player
- **Timed Flame Wall Sections**: Creates temporary flame walls
- **Configurable Damage Rate**: Adjustable damage values
- **Mob Spawn Control**: Manages mob spawning behavior

## Installation

1. Build the plugin using Maven: `mvn clean package`
2. Copy the generated JAR file from `target/` to your server's `plugins/` folder
3. Restart your Minecraft server

## Usage

- Give yourself the Fire Mace: `/firemace`
- Right-click with the Fire Mace to activate special abilities
- Move near lava traps, flame walls, or spawn zones to take heat damage

## Permissions

- `firemace.give` - Allows player to give themselves the Fire Mace (default: op)

## Configuration

The plugin includes configurable damage rates and effect durations within the source code. Modify the values in `PlayerListener.java` to adjust behavior.