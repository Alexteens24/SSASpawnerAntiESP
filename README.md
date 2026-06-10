# SSASpawnerAntiESP

[![Build](https://github.com/Alexteens24/SSASpawnerAntiESP/actions/workflows/build.yml/badge.svg)](https://github.com/Alexteens24/SSASpawnerAntiESP/actions/workflows/build.yml)

Addon for [SmartSpawner](https://github.com/NighterDevelopment/SmartSpawner) that hides spawner blocks from players without line of sight. Uses async DDA ray tracing and per-player block update packets — much lighter than scanning entire chunks (e.g. RayTraceAntiXray).

**Folia-supported.**

## Requirements

| Component | Version |
|-----------|---------|
| Paper | `1.21.11` or `26.1.2` |
| SmartSpawner | `1.6.2+` (Paper 1.21.11) / `1.6.7+` (Paper 26.1.2) |
| Java (server) | 21 or 25 (match your Paper build) |

Download the JAR that matches your Paper version from [GitHub Actions artifacts](https://github.com/Alexteens24/SSASpawnerAntiESP/actions) or [Releases](https://github.com/Alexteens24/SSASpawnerAntiESP/releases).

| Paper version | JAR classifier |
|---------------|----------------|
| 1.21.11 | `SSASpawnerAntiESP-*-1.21.11.jar` |
| 26.1.2 | `SSASpawnerAntiESP-*-26.1.2.jar` |

## Installation

1. Install **SmartSpawner** on your Paper server.
2. Drop the matching `SSASpawnerAntiESP-*.jar` into `plugins/`.
3. Restart the server (or load after SmartSpawner).
4. Edit `plugins/SSASpawnerAntiESP/config.yml` if needed.

## How it works

1. On enable, indexes all spawner locations from the SmartSpawner API.
2. Each player gets async ray traces from their eye position to nearby spawners.
3. Spawners without line of sight are replaced on the **client only** with a decoy block (stone / deepslate / netherrack / end stone).
4. When line of sight opens, the real spawner block is sent back.

Server-side `SpawnerData` and admin commands like `/ss list` are unchanged — only the player's view is spoofed.

## Configuration

```yaml
settings:
  update-ticks: 1              # Ticks between block update packets per player
  ms-per-ray-trace-tick: 50    # Async ray trace interval (ms)
  ray-trace-threads: 1         # Worker threads for ray tracing

world-settings:
  default:
    enabled: true
    ray-trace-distance: 64.0   # Max distance to check spawners
    rehide-blocks: true        # Skip ray trace beyond rehide-distance
    rehide-distance: 60.0
    section-leap: false        # Skip empty 16³ sections (faster, enable after testing)
```

Per-world overrides: `world-settings.<world-name>.<option>`.

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/ssaspawnerantiesp reload` | `ssaspawnerantiesp.command.reload` | Reload config and re-index spawners |

## Build locally

```bash
# Paper 26.1.2 (default)
./gradlew jar

# Paper 1.21.11
./gradlew jar -PpaperTarget=1.21.11
```

Output: `build/libs/SSASpawnerAntiESP-<version>-<paperTarget>.jar`

## Development

- NMS shims live under `src/nms/paper-<version>/`.
- Add a new Paper target in `build.gradle.kts` (`paperTargets` map) and a matching `NmsCompat` source set.

## License

No license specified yet.
