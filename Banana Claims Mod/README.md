# Banana Claims

Banana Claims is a production-focused Fabric claim system built for the Banana Sandwich SMP settlement model. It provides player-managed land claims, configurable notifications, opt-in protections, client-only visual boundaries, BlueMap markers, role management, and administrative recovery tools.

## Highlights

- Server-side operation; players do not need to install Banana Claims
- Multi-chunk and irregular claims
- Owner, subowner, and member roles
- Terrain-following 3D boundary previews visible only to the requesting player
- Configurable preview materials, glow, fade, and pulse animation
- Enter and leave notifications through action bar, title, or chat
- Optional BlueMap polygon markers with owner and membership details
- Opt-in protection flags for blocks, interactions, containers, entities, PvP, and explosions
- LuckPerms integration through Fabric Permissions API, with vanilla permission-level fallbacks
- Administrative force transfer, force delete, reload, lookup, and diagnostics commands
- JSON persistence with atomic file replacement

## Requirements

- Minecraft `26.2`
- Fabric Loader `0.19.3` or newer
- Fabric API `0.154.1+26.2` or compatible
- Java `25` or newer

Optional integrations:

- BlueMap
- Fabric Permissions API v0
- LuckPerms

## Installation

1. Install Fabric Loader and Fabric API on the server.
2. Place the Banana Claims JAR in the server's `mods` directory.
3. Optionally install BlueMap, Fabric Permissions API, and LuckPerms.
4. Start the server once to generate configuration files.
5. Review:
   - `config/bananaclaims.json`
   - `config/bananaclaims-preview.json`
   - `config/bananaclaims/claims.json`

Banana Claims sends vanilla packets for previews, so no client mod is required.

## Quick Start

```text
/claim create Home
/claim pos1
/claim pos2
/claim createarea Settlement
/claim preview
/claim flag Settlement containers true
/claim member add Settlement PlayerName
```

Protection is disabled by default. Enable only the flags wanted for each claim.

## Documentation

- [Commands](COMMANDS.md)
- [Protection Flags](FLAGS.md)
- [Configuration](CONFIGURATION.md)
- [Permissions](PERMISSIONS.md)
- [Performance and Architecture](PERFORMANCE.md)
- [Release Checklist](RELEASE_CHECKLIST.md)
- [Development Status](DEVELOPMENT.md)

## Building From Source

```powershell
.\gradlew.bat clean build
```

Built JARs are written to `build/libs/`.

## Data Safety

Claim data is stored in `config/bananaclaims/claims.json`. Back up the entire `config/bananaclaims` directory before major server upgrades or manual data edits. If this file cannot be parsed during startup, Banana Claims refuses to initialize rather than risking an empty-data overwrite.

## License

Banana Claims currently uses the repository's CC0-1.0 license file.
