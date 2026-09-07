# BTA Map

Client-side minimap, world map, and waypoints for Better Than Adventure 8.0.1 (Babric).

## Features

- Minimap HUD component (square or round, resizable, optional rotation, coordinates). Move and configure it in the vanilla HUD editor.
- Fullscreen world map (`M`): scroll to zoom, drag to pan, right-click to add or edit a waypoint.
- Waypoints (`B` to add at your position): shown on the minimap, the world map, and as in-world beams with distance labels.
- Map data is built from loaded chunks using texture atlas colors and persisted as PNG regions under `.minecraft/btamap/<world>/dim<id>/`. Waypoints live in `.minecraft/btamap/<world>/waypoints.json`.

## Keys

| Key | Action |
| --- | --- |
| `M` | Open world map |
| `B` | New waypoint |
| `=` / `-` | Minimap zoom in / out |
| unbound | Toggle minimap |

All keys and options are configurable through the vanilla options and HUD editor screens.

## Building

Requires JDK 21 (Gradle toolchain) and targets Java 17.

```
./gradlew build
```

The jar lands in `build/libs/`. Depends on [HalpLibe](https://github.com/Turnip-Labs/bta-halplibe).

## License

MIT
