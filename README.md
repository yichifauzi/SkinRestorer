# SkinRestorer

![GitHub License](https://img.shields.io/github/license/Suiranoil/SkinRestorer)
![Build](https://github.com/Suiranoil/SkinRestorer/actions/workflows/build.yml/badge.svg)
![Enviroment](https://img.shields.io/badge/enviroment-server-orangered)
[![CurseForge Downloads](https://cf.way2muchnoise.eu/skinrestorer.svg)](https://www.curseforge.com/minecraft/mc-mods/skinrestorer)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/skinrestorer?logo=modrinth)](https://modrinth.com/mod/skinrestorer)

SkinRestorer is a **server-side** only mod for Fabric that allows players to use and change skins on servers running in
offline/insecure mode.

## Features

- **Set Skins from Mojang Account**: Fetch and apply skins using a valid Minecraft account name.
- **Set Skins from URL**: Apply skins from any image URL, supporting both classic (Steve) and slim (Alex) skin models.

## Command Usage Guide

### Set Mojang Skin

```
/skin set mojang <skin_name> [<targets>]
```

- **Parameters:**
    - `<skin_name>`: Minecraft account name to fetch the skin from.
    - `[<targets>]`: (Optional, server operators only) Player(s) to apply the skin to.

### Set Web Skin

```
/skin set web (classic|slim) "<url>" [<targets>]
```

- **Parameters:**
    - `(classic|slim)`: Type of the skin model (`classic` for Steve model, `slim` for Alex model).
    - `"<url>"`: URL pointing to the skin image file (ensure it follows Minecraft's skin size and format requirements).
    - `[<targets>]`: (Optional, server operators only) Player(s) to apply the skin to.

### Clear Skin

```
/skin clear [<targets>]
```

- **Parameters:**
    - `[<targets>]`: (Optional, server operators only) Player(s) to clear the skin for.

### Notes:

- If `targets` is not specified, the command will apply to the player executing the command.

### Examples:

```
/skin set mojang Notch
/skin set web classic "https://example.com/skin.png"
/skin clear @a
```
