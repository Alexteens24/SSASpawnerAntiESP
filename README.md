# SSASpawnerAntiESP

**Tiếng Việt:** [README.vi.md](README.vi.md)

[![Build](https://github.com/Alexteens24/SSASpawnerAntiESP/actions/workflows/build.yml/badge.svg)](https://github.com/Alexteens24/SSASpawnerAntiESP/actions/workflows/build.yml)

Addon for [SmartSpawner](https://github.com/NighterDevelopment/SmartSpawner) that **hides spawner blocks from players without line of sight** (anti-ESP / spawner x-ray).

Players cannot see spawners through walls — their client receives a decoy block (stone, deepslate, etc.) instead. When line of sight is clear, the real spawner is shown again.

> **Note:** This plugin only changes **each player's client view**. Server-side spawner data, SmartSpawner, and admin commands (e.g. `/ss list`) are **not affected**.

Supports **Paper** and **Folia**.

![Showcase](showcase.gif)

---

## Requirements

| Component | Version |
|-----------|---------|
| Server | **Paper** `1.21.11` or `26.1.2` |
| SmartSpawner | `1.6.2+` (Paper 1.21.11) · `1.6.7+` (Paper 26.1.2) |
| PacketEvents | `2.12.1+` |
| Java | `21` (Paper 1.21.11) · `25` (Paper 26.1.2) |

**SmartSpawner** and **PacketEvents** must be installed first. SSASpawnerAntiESP disables itself if the SmartSpawner API is unavailable.

---

## Download

One **universal JAR** works on all supported Paper versions (no classifier suffix). Version-specific NMS bindings (`paper_1_21_11`, `paper_26_1_2`) are bundled and selected at runtime via `NmsBridge` (same approach as [RayTraceAntiXray](https://github.com/Alexteens24/RayTraceAntiXray)).

| File name |
|-----------|
| `SSASpawnerAntiESP-<version>.jar` |

Available from:

- [Releases](https://github.com/Alexteens24/SSASpawnerAntiESP/releases) (official builds)
- [GitHub Actions](https://github.com/Alexteens24/SSASpawnerAntiESP/actions) → latest workflow run → **Artifacts**

Build locally: `./gradlew shadowJar` → `build/libs/SSASpawnerAntiESP-<version>.jar`

---

## Installation

1. Install **SmartSpawner** and **PacketEvents**, then start the server once.
2. Place the JAR in the `plugins/` folder.
3. Restart the server.
4. (Optional) Edit `plugins/SSASpawnerAntiESP/config.yml`, then run `/ssaspawnerantiesp reload`.

---

## How it works

Uses an architecture similar to [RayTraceAntiXray](https://github.com/AdvancedAntiXray/RayTraceAntiXray), adapted for spawners:

1. **Chunk obfuscation** — when Paper sends a chunk to the client, spawners are replaced with decoy blocks (stone, deepslate, etc.) in the packet; spawner block entities are stripped from the packet.
2. **PacketEvents** — syncs the spawner list for ray tracing after the chunk packet is sent.
3. **Async ray trace** — checks line of sight from the player's eye (and third-person camera if enabled) to each spawner in loaded chunks.
4. **Block updates** — clear LOS → send the real spawner; obstructed → keep the decoy on the client.
5. **Join / teleport** — nearby spawners are hidden immediately (from the SmartSpawner index) to prevent a flash before chunk obfuscation applies.

The SmartSpawner index tracks place/break and enables fast hide on join; runtime show/hide is driven mainly by real spawner blocks in chunks.

Decoy blocks by dimension:

| Dimension | Decoy block |
|-----------|-------------|
| Overworld (y ≥ 0) | Stone |
| Overworld (y < 0) | Deepslate |
| Nether | Netherrack |
| The End | End Stone |

---

## Configuration

File: `plugins/SSASpawnerAntiESP/config.yml`

### `settings` — global

| Option | Default | Description |
|--------|---------|-------------|
| `update-ticks` | `1` | Ticks between block update packets per player. |
| `ms-per-ray-trace-tick` | `50` | Interval (ms) between line-of-sight checks. |
| `ray-trace-threads` | `1` | Worker threads for line-of-sight checks. Increase on busy servers. |

### `world-settings` — per world

Defaults live under `world-settings.default`. Override per world: `world-settings.<world-name>.<option>`.

| Option | Default | Description |
|--------|---------|-------------|
| `enabled` | `true` | Enable or disable the plugin in that world. |
| `ray-trace-distance` | `64.0` | Max distance (blocks) to check spawners around a player. |
| `ray-trace-third-person` | `false` | Also ray trace from third-person (F5) camera — useful when the camera offset differs from the eye. |
| `rehide-blocks` | `true` | Optimization: spawners beyond `rehide-distance` are hidden without a ray trace. |
| `rehide-distance` | `60.0` | Distance threshold (blocks) for the `rehide-blocks` optimization. |
| `section-leap` | `false` | Skip all-air 16×16×16 sections during ray tracing (faster). Enable only after testing on your server. |
| `max-ray-trace-block-count-per-chunk` | `64` | Max spawners to ray trace per chunk (Paper obfuscation limit). |

Example — disable in world `spawn`:

```yaml
world-settings:
  spawn:
    enabled: false
```

---

## Commands & permissions

| Command | Permission | Description |
|---------|------------|-------------|
| `/ssaspawnerantiesp reload` | `ssaspawnerantiesp.command.reload` | Reload config and re-index spawners |

---

## Limitations

- Only hides the **spawner block** on the client — not a complete anti-cheat (outline mods, particles, etc. may still be vectors).
- **Do not run alongside [RayTraceAntiXray](https://github.com/AdvancedAntiXray/RayTraceAntiXray)** on the same world — both replace Paper's `chunkPacketBlockController`.
- When `enabled: false` for a world, the plugin restores Paper's default ore anti-xray controller (if the server uses engine-mode `HIDE`).
- Decoy blocks may **not match** surrounding terrain (e.g. stone among dirt/sand) — a trade-off of packet-based hiding.

---

## License

[MIT](LICENSE)
