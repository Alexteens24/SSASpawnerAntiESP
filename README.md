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
| Java | `21` (Paper 1.21.11) · `25` (Paper 26.1.2) |

**SmartSpawner must be installed first.** SSASpawnerAntiESP disables itself if the SmartSpawner API is unavailable.

---

## Download

Use the **correct JAR** for your Paper version:

| Paper version | File name |
|---------------|-----------|
| 1.21.11 | `SSASpawnerAntiESP-*-1.21.11.jar` |
| 26.1.2 | `SSASpawnerAntiESP-*-26.1.2.jar` |

Available from:

- [Releases](https://github.com/Alexteens24/SSASpawnerAntiESP/releases) (official builds)
- [GitHub Actions](https://github.com/Alexteens24/SSASpawnerAntiESP/actions) → latest workflow run → **Artifacts**

---

## Installation

1. Install **SmartSpawner** and start the server once.
2. Place the matching JAR in the `plugins/` folder.
3. Restart the server.
4. (Optional) Edit `plugins/SSASpawnerAntiESP/config.yml`, then run `/ssaspawnerantiesp reload`.

---

## How it works

1. On enable, loads all spawner coordinates from SmartSpawner.
2. Periodically checks from each player's eye position to nearby spawners — whether blocks obstruct the view.
3. **Not visible** → sends a packet to replace the spawner block with a decoy on that player's client.
4. **Visible** → sends the real spawner block back.
5. On join or teleport, nearby spawners are hidden immediately to prevent a brief flash before the check completes.

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
| `rehide-blocks` | `true` | Optimization: spawners beyond `rehide-distance` are hidden without a ray trace. |
| `rehide-distance` | `60.0` | Distance threshold (blocks) for the `rehide-blocks` optimization. |
| `section-leap` | `false` | Skip all-air 16×16×16 sections during ray tracing (faster). Enable only after testing on your server. |

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
- Decoy blocks may **not match** surrounding terrain (e.g. stone among dirt/sand) — a trade-off of packet-based hiding.
- You must use the **correct JAR** for your Paper version; the wrong build may fail to load or error at runtime.

---

## License

[MIT](LICENSE)
