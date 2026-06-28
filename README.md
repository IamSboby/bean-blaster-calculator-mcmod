# Bean Blaster Calculator

[![GitHub release](https://img.shields.io/github/v/release/IamSboby/bean-blaster-calculator-mcmod?style=flat&color=181717)](https://github.com/IamSboby/bean-blaster-calculator-mcmod/releases)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=flat&logo=minecraft&logoColor=white)](https://minecraft.net)
[![Fabric](https://img.shields.io/badge/Fabric_Loader-0.16.9+-DBB589?style=flat)](https://fabricmc.net)
[![License](https://img.shields.io/github/license/IamSboby/bean-blaster-calculator-mcmod?style=flat&label=License)](LICENSE)
[![Discord](https://img.shields.io/badge/Discord-Reversal_-5865F2?style=flat&logo=discord&logoColor=white)](https://discord.gg/jxQtq3XBKv)

A lightweight client-side Fabric mod for Minecraft 1.21.11 that calculates how many Wind Charges are required in each Bean Blaster dispenser to launch an Ender Pearl toward a target X/Z position.

The mod ports the original Bean Blaster / Pearl Launcher calculator logic directly into Minecraft, so you can run the calculation in-game without opening Python or leaving the server.

## Features

- Client-side only.
- Works in single-player and multiplayer without server-side installation.
- Saves a Bean Blaster setup per world, server and dimension.
- Detects the magma block you are looking at during setup.
- Calculates dispenser Wind Charge amounts from target X/Z coordinates.
- Shows raw Wind Charge counts and Minecraft-friendly inventory amounts.
- Avoids command conflicts by using `/bb` and `/beanblaster` instead of `/blaster`.
- Does not open any custom GUI screen.
- Stores readable JSON config in the Minecraft `config` folder.

## Commands

### Setup

```text
/bb setup
```

or:

```text
/beanblaster setup
```

Look directly at the Bean Blaster's magma block and run the setup command. The mod saves the magma block position as the reference point for future calculations.

The saved setup includes:

- X coordinate
- Y coordinate
- Z coordinate
- Dimension
- Current single-player world or multiplayer server

### Calculate a target

```text
/bb <x> <z>
```

Example:

```text
/bb 1250 -840
```

or:

```text
/beanblaster 1250 -840
```

The mod calculates which dispensers must be loaded and how many Wind Charges each one needs.

### Help

```text
/bb help
```

or:

```text
/beanblaster help
```

Shows the available commands and explains how to use the mod.

## Output format

Version 1.0.16 shows both the raw amount and the Minecraft inventory format.

Example:

```text
Dispenser 1: 124 (1 Stack + 60 Items) Wind Charges
```

Another example:

```text
Dispenser 2: 2000 (1 SB + 4 Stacks + 16 Items) Wind Charges
```

Inventory conversion:

```text
1 Stack = 64 items
1 SB = 27 Stacks = 1728 items
```

`SB` means Shulker Box.

## Why the command is `/bb` instead of `/blaster`

This mod intentionally uses:

```text
/bb
/beanblaster
```

instead of:

```text
/blaster
```

This avoids conflicts with other mods that may already register `/blaster`. If another installed mod owns `/blaster`, using `/bb` prevents command collisions and GUI/render crashes from unrelated mods.

## Configuration

The setup is saved in:

```text
.minecraft/config/bean-blaster-calculator.json
```

The config separates setups by:

- Single-player world
- Multiplayer server address
- Dimension

Example structure:

```json
{
  "singleplayer:New World": {
    "minecraft:overworld": {
      "x": 0,
      "y": 64,
      "z": 0
    }
  },
  "multiplayer:play.example.net": {
    "minecraft:overworld": {
      "x": 1200,
      "y": 64,
      "z": -900
    }
  }
}
```

If the config file is missing, the mod creates a new one automatically when setup is saved.

## Installation

Install the mod on the client only.

Required:

- Minecraft 1.21.11
- Fabric Loader
- Fabric API
- Java 21

Steps:

1. Build or download the mod `.jar`.
2. Put the `.jar` in your Minecraft `mods` folder.
3. Make sure Fabric API is also installed.
4. Launch Minecraft with Fabric.
5. Join a world or server.
6. Use `/bb setup` while looking at the magma block.

## Building from source

From the project root, run:

```powershell
.\gradlew.bat remapJar
```

On Linux/macOS:

```bash
./gradlew remapJar
```

The finished mod will be generated in:

```text
build/libs/
```

Expected output name:

```text
bean-blaster-calculator-1.0.16.jar
```

## Compatibility

- Minecraft: 1.21.11
- Java: 21
- Fabric Loader: 0.18.x or newer compatible 1.21.11 builds
- Fabric API: compatible with Minecraft 1.21.11
- Side: client only

This mod does not require the server to install anything.

## Known limitations

- The calculator only uses X/Z target coordinates.
- The setup block must be a magma block.
- If the target falls into an invalid or unreachable sector, the mod warns instead of giving unsafe dispenser values.
- The calculation follows the original Bean Blaster formula behavior, including integer truncation of final dispenser values.

## Credits

Original "Pearl Launcher" concept:

```text
@IddieBean
```

Mod creators:

```text
wavedtime
Sboby_
Albuz999
```

## Website

```text
https://modrinth.com/mod/bean-blaster-calc
```

## License

MIT License.
