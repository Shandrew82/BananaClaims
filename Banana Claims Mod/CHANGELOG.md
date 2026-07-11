# Changelog

## 1.0.0 Release Candidate

### Added

- Multi-chunk and irregular claims
- Owner, subowner, and member lifecycle management
- Client-isolated Renderer V2 with configuration, fade, and pulse
- Opt-in block, interaction, container, entity, PvP, and explosion protection
- Administrative lookup, diagnostics, force transfer, force delete, and reload tools
- Granular permission nodes with Fabric Permissions API and LuckPerms support
- Server-resolved language catalog for vanilla-client compatibility
- Release documentation and test checklist

### Improved

- BlueMap now updates individual changed claims rather than scanning all claims for every event
- BlueMap supports multiple disconnected claim outlines
- BlueMap details use clearer role sections and escaped HTML
- Claim data writes now use temporary-file replacement
- Reserved, unenforced flags are hidden from player flag commands
- Placeholder template client code and example mixins were removed

### Known Release-Candidate Gap

- The current uploaded source does not contain the previously documented invitation system. This must be restored, reimplemented, or explicitly deferred before declaring the final 1.0 feature set.
