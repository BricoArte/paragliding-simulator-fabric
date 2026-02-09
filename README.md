# Paragliding Simulator (Fabric)

[Leer en espanol](README.es.md)

<img align="left" alt="Mod logo" width="128" height="128" src="src/main/resources/assets/paraglidingsimulator/icon.png">

Minecraft Fabric mod that adds **realistic paragliding**, thermals, and launch sites with a dynamic wind system.  
Based on **Skydiving Mod** by tgskiv, expanded with paraglider, thermals, and worldgen.

---

## Features

- **Paraglider** with its own controllable flight physics.
- **Realistic wind physics**: wind affects speed and drift. *(original from Skydiving Mod)*
- **Dynamic wind**: direction and speed change gradually. *(original from Skydiving Mod)*
- **Wind forecast** with upcoming changes. *(original from Skydiving Mod)*
- **Server-client wind sync**. *(original from Skydiving Mod)*
- **Windsock** to check wind direction. *(original from Skydiving Mod)*
- **Thermals**: rising air columns with visible clouds.
- **Launch sites (structures)**: natural takeoff points in high/mountain areas.
- **HUD and vario/helmet sounds** configurable.

Note: in the original mod, wind strongly affects elytra flight; in this mod that effect is reduced to give more focus to paragliding.

---

## Requirements

- **Minecraft** 1.21.2
- **Fabric Loader** latest stable for Minecraft 1.21.2
- **Fabric API** (required)

Optional recommended:
- **Mod Menu** (shows Mods button and shortcuts)
- **Cloth Config** (needed to open the full settings screen)
 
Tested with Fabric Loader 0.16.14 and 0.18.4.

---

## Installation

1. Install the latest stable Fabric Loader for 1.21.2.
2. Put the mod .jar into `.minecraft/mods`.
3. Install Fabric API (and Mod Menu/Cloth Config if you want advanced settings).
4. Launch Minecraft.

---

## What the mod adds

Main items:
- **Paraglider**: lets you fly with realistic control.
- **Flight helmet**: shows HUD and allows configuration.
- **Vario**: audio feedback for climb/sink.
- **Windsock**: indicates wind direction. *(original from Skydiving Mod)*
- **Poster/book**: quick flight guide.

---

## Crafting and first steps

- Check recipes with **REI/JEI/EMI**.
- Craft a **windsock** first to learn wind direction.
- Prepare the **helmet** or **vario** for climb/sink feedback.
- Craft the **paraglider** and take off from high ground.
- Optional: search for **launch sites** in the mountains.

---

## Basic flight controls

- **Accelerate**: more speed, more sink.
- **Brake**: reduces speed, less sink.
- **Turn**: adjust heading to align with wind.
- **Spin**: fast descent maneuver (use with care).

Tips:
- Always take off **into the wind**.
- Avoid spin close to the ground.
- Use windward slopes and thermals to gain altitude.

---

## Launch site structures (worldgen)

- Generated in high/mountain terrain.
- You will find a windsock, a training poster, and a chest.
- If you cannot find one, you can use `/locate` or the mod test command (if you have permissions).

---

## Thermals

- Thermals are rising air columns.
- They create small clouds at their top.
- Cloud size depends on thermal size and strength.
- Each day has a random variation in thermal size and max height.
- Presets for **amount**, **intensity**, **height**, and **size**.
- Configurable for performance or realism.

---

## Configuration

Two ways to open the menu:

1. **From the flight helmet**: its menu shows the **"Mod Settings"** button.
2. **From Mod Menu** (if installed): the "Mods" button in the main menu.

If Cloth Config is missing, the button will warn in chat.

---

## Useful commands

| Command | Description |
| --- | --- |
| `/wind forecast` | View the next 5 wind changes *(original from Skydiving Mod)* |
| `/wind again` | Regenerate the forecast *(original from Skydiving Mod)* |
| `/wind hud true/false` | Wind debug HUD *(original from Skydiving Mod)* |

---

## Performance and compatibility

- Increasing thermal count or generation distance can reduce FPS.
- If the Mods button is missing, install **Mod Menu**.
- If settings do not open, install **Cloth Config**.

---

## Development (devs only)

Requirements:
- Java 21
- Gradle 8

Build:

```bash
./gradlew build
```

The generated .jar is in `build/libs/`.

---

## License

This repository is licensed under **MIT** (see `LICENSE`).

---

## Credits

- Based on **Skydiving Mod** by **tgskiv**: https://github.com/tgskiv/skydiving-mod-fabric
- Original code (wind, forecast, windsock, sync) comes from that project.
- This mod extends it with paraglider, thermals, launch sites, and related gameplay.

