# ServerSniffer — Meteor Addon

> Reads every server packet to tell you **everything** about the server: software, proxy, plugins, anticheat, limits, world & security.

A **Meteor Client addon** for Minecraft 1.21.4. Drops into your `mods/` alongside Meteor — adds a new `ServerSniffer` category, commands, and an in-game HUD.

---

## What it tells you

| Section | How it knows |
|---|---|
| **MOTD / version / player counts / icon** | Active status ping (SRV-aware) |
| **Software** (Paper, Purpur, Spigot, Fabric, Velocity, Waterfall, BungeeCord, Sponge, Folia …) | `minecraft:brand` payload + channel heuristics |
| **Proxy** (Velocity / BungeeCord / Waterfall) | `velocity:player_info`, `bungeecord:main`, version string |
| **Plugins** (WorldGuard, EssentialsX, etc.) | `minecraft:register` channels + known mappings — unknown channels listed too |
| **Anticheat** (AAC, NCP, Spartan, Matrix, Vulcan, Grim, Intave …) | AC channels + behavioural hints |
| **ViaVersion / protocol translation** | Version-range in status + Via channels |
| **Limits** — view / simulation distance, compression threshold | `GameJoin` + `ChunkLoadDistance` / `SimulationDistance` |
| **World** — dimension, difficulty, hardcore, reducedDebugInfo, seed hash | `GameJoin` / `CommonPlayerSpawnInfo` |
| **Security** — enforcesSecureChat, hardcore | `GameJoin` flags |
| **Timings** — KeepAlive cadence, total S2C count, top 8 packet types | Live `ClientConnection` tap |
| **Rolling packet log** (last 400 S2C names) | Same tap |

---

## Module

`ServerSniffer` category → **Server Sniffer** module

- **log-packets** — keep rolling packet log
- **verbose-on-join** — 1-line chat summary on every join
- **reset-on-leave** — clear intel on disconnect so the next server starts clean
- **notify-software / anticheat / plugins** — optional toasts / chat pings
- **auto-ping** — pings `status` 1s after join so MOTD/version are filled instantly (uses MC's own `MultiplayerServerListPinger`, so SRV & all)

---

## Commands

```
.serverinfo              full pretty report in chat
.serverinfo copy         copy plaintext report to clipboard
.serverinfo dump         save to  serversniffer/report_<ip>_<ts>.txt
.serverinfo clear        clear sniffed data (fresh sniff)
.serverinfo log          rolling packet log (last 60 shown, full in dump)
.serverinfo channels     list every plugin channel seen
.serverinfo plugins      inferred plugins only

.scan                    ping the server you're on (refresh MOTD/version)
.scan <address[:port]>   ping any server without joining it
```
Aliases: `.si`, `.sniff`, `.ping`, `.probe`

---

## HUD

`HUD → Add Element → ServerSniffer → Server HUD`

- **Compact** — software, proxy, plugin/channel counts, anticheat
- **Full** — plus distances, packet counts, KeepAlive cadence
- Toggle IP display, move/resize like any Meteor HUD element

---

## How it works (internals)

- **`ClientConnectionMixin`** — taps `channelRead0` so every `S2CPacket` is classified before handling; extracts `CustomPayload` (brand, `minecraft:register`), `KeepAlive`, `CommonPing` and feeds `PluginDetector` / `AnticheatDetector` / `SoftwareFingerprinter`
- **`ClientPlayNetworkHandlerMixin`** — hooks `onGameJoin`, `onPlayerRespawn`, `onChunkLoadDistance`, `onSimulationDistance`, `onDifficulty`, `onSynchronizeTags`, `onDisconnected` for world/limit fields
- **`ServerPinger`** — wraps `MultiplayerServerListPinger` to hit the status endpoint (same path the multiplayer screen uses)
- No server exploit / no packet spam — just reads what the server already sends you. Safe to leave on everywhere.

---

## Build

```bash
# prerequisites: Java 21, Gradle wrapper will fetch itself
./gradlew build          # -> build/libs/serversniffer-1.2.0.jar

# dev run (launches MC with Meteor + this addon)
./gradlew runClient
```

> **Meteor version must match your MC version.** For MC 1.21.4 use Meteor `0.5.8`. Update `build.gradle`'s `meteor-client` line if you target a different MC.

---

## Install

1. Install **Fabric Loader** + **Fabric API** + **Meteor Client** for 1.21.4.
2. Drop `serversniffer-1.2.0.jar` into `mods/` next to `meteor-client-*.jar`.
3. Launch — you'll see a `ServerSniffer` category in Meteor's GUI.

---

## Project layout

```
src/main/java/com/serversniffer/
  ServerSnifferAddon.java            entrypoint, registers module/commands/HUD
  core/
    ServerIntel.java                 all intel about the current server
    IntelStore.java                  global singleton
  fingerprint/
    SoftwareFingerprinter.java       brand → software guess
    PluginDetector.java              channel → plugin name (+ unknown channels)
    AnticheatDetector.java           channel/behaviour → anticheat
  mixin/
    ClientConnectionMixin.java       S2C packet tap
    ClientPlayNetworkHandlerMixin.java world/limits hook
  report/ReportGenerator.java        pretty chat/file report
  util/
    ServerPinger.java                status ping wrapper
    TextUtil.java                    small helpers
  modules/ServerSnifferModule.java  Meteor module
  commands/
    ServerInfoCommand.java           .serverinfo
    ScanCommand.java                 .scan
  hud/ServerHud.java                 in-game HUD element
```

---

## Limitations & notes

- **Plugins**: only plugins that expose a plugin channel appear. Many papers hide channels — unknown channels are listed so you can identify custom ones by name.
- **MOTD on first join** may show "(not captured)" if the status ping hasn't returned yet — wait 1-2s or run `.scan`.
- This addon is **client-side only** and never sends packets beyond the vanilla handshake/status flow.

---

## License

MIT — do what you want.
