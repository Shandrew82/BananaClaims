# Banana Claims Development

## Project Information

- Project: Banana Claims
- Type: Server-side Fabric mod
- Minecraft: 26.2
- Fabric Loader: 0.19.3+
- Fabric API: 0.154.1+26.2
- Java: 25+

## Development Philosophy

- Large, logical implementation batches
- Complete replacement files and project-structured ZIP delivery
- Minimize rebuild and server-upload cycles
- Clean, reusable architecture
- Vanilla-quality behavior
- Performance-first and event-driven where practical
- No new milestone until the current milestone builds and passes in-game testing

---

# Current Status

## Estimated Completion

Approximately 98% toward the final 1.0 release candidate.

The renderer, lifecycle, protection, administration, permissions, configuration, BlueMap, localization foundation, and release documentation milestones are complete. The next step is a complete release-candidate audit and a decision on the missing invitation subsystem.

## Current Milestone

**Banana Claims 1.0 Release Candidate Audit**

Current goals:

- Run the complete release checklist
- Resolve any compile or runtime regressions
- Confirm upgrade safety using existing server data
- Decide whether invitations are required for 1.0 or formally deferred
- Update the final version/changelog and prepare the distributable JAR

---

# Completed Systems

## Claims

- Single-chunk creation
- Multi-chunk rectangular creation
- Irregular expansion and shrinking
- Stable claim IDs
- Rename
- Description
- Info and list
- Delete
- Chunk-indexed lookup
- JSON persistence
- Atomic claim-data replacement

## Roles and Lifecycle

- Owner role
- Subowner role
- Member role
- Role invariants and automatic repair
- Add/remove members
- Promote/demote subowners
- Member leave
- Subowner step-down
- Owner leave safeguards
- Ownership transfer
- Administrative force transfer
- Persistence and BlueMap refresh after role changes

## Notifications

- Enter and leave detection
- Action bar, title, and chat display modes
- Custom enter/leave titles and subtitles
- Hex colors and manual per-character gradients
- Configurable sounds
- Claim placeholders

## Renderer V2

Production renderer completed:

- Terrain following
- Existing claim previews
- Selection and automatic previews
- Irregular outlines
- Tree, log, and leaf filtering
- Ground borders
- Corner anchors
- Corner columns to build limits
- Guide columns
- Underground visibility
- Packet-only client-isolated displays
- No persistent world entities
- Configurable materials and dimensions
- Configurable glow and occlusion behavior
- Fade animation
- Pulse animation
- Preview stop, replacement, expiry, dimension, and disconnect cleanup
- Legacy particle renderer removed

Preview config version: **4**

## Protection

Protection remains disabled by default.

Implemented opt-in flags:

- Block breaking
- Block placement
- Doors, trapdoors, gates, buttons, levers, pressure plates, tools, and utility interactions
- Block and vehicle containers
- Entity interaction
- Entity placement
- Player-attributed entity damage
- PvP
- Explosion block, entity, player, and knockback protection
- Cross-boundary explosion filtering
- Owner, subowner, member, and permission bypass
- Denial-message cooldowns

Reserved persisted fields not exposed to players:

- Fire spread
- Mob griefing

## BlueMap

- Runtime detection with no hard dependency
- Dimension-aware polygon markers
- Incremental per-claim updates
- Full synchronization after claim-data load
- Multiple disconnected outlines
- Escaped HTML details
- Owner, subowner, member, chunk, and description display
- Automatic removal after deletion

## Administration

- Claim list and lookup
- Nearest claim lookup
- Stable selectors for duplicate names
- Force transfer
- Confirmed force delete
- Main config reload
- Claim-data reload
- Preview config reload
- Full reload
- Global and per-claim diagnostics
- Audit logging

## Configuration and Permissions

- Main config manager with validation and atomic writes
- Preview config manager with validation and migration
- Fabric Permissions API/LuckPerms bridge
- Configurable vanilla fallback levels
- Exact-node fallback overrides
- Granular player, management, and admin nodes
- Global protection bypass permission
- Flag tab completion from the canonical flag registry
- Boolean tab completion

## Localization

- Bundled `en_us.json` language catalog
- Normal player command messages moved into the catalog
- Protection denial messages moved into the catalog
- BlueMap labels moved into the catalog
- Server-side resolution so vanilla clients never receive raw custom translation keys

## Documentation and Production Cleanup

- Release-ready README
- Command reference
- Flag reference
- Configuration guide
- Permission guide
- Performance/architecture review
- Changelog
- Release checklist
- Placeholder client entrypoint and example mixins removed
- Misplaced `ClaimSubOwner` source moved into the claim package

---

# Known Release-Candidate Gap

## Invitation System

Older development notes described invite, accept, deny, timeout, and expiration behavior. The current uploaded source tree contains no invitation package, invitation commands, or pending-invitation storage.

Do not claim that invitations are included in 1.0 until one of these decisions is made:

1. Restore or reimplement the invitation system before release.
2. Explicitly defer invitations to a post-1.0 milestone and update player documentation accordingly.

---

# Current Architecture

## Core Managers

- `ClaimManager`: claim identity, chunk lookup, roles, lifecycle, and change events
- `ClaimStorage`: claim JSON persistence
- `BananaClaimsConfigManager`: main config
- `PreviewV2ConfigManager`: preview config and resolved materials
- `ClaimPermissionService`: external permissions and vanilla fallbacks
- `ClaimProtectionService`: centralized protection policy
- `ClaimProtectionManager`: Fabric callbacks and denial messaging
- `DisplayPreviewV2Manager`: packet-only preview lifecycle and animation

## Event Flow

Claim mutations publish typed change events. BlueMap consumes those events and performs claim-specific updates whenever an event includes a claim.

## Book GUI Architecture

The future Book GUI remains a post-1.0 project. It should use the existing claim managers and configuration models rather than duplicating business logic.

Planned sections:

- Notifications
- Members
- Permissions
- Appearance
- Preview
- Transfer

---

# Post-1.0 Roadmap

- Book GUI
- Invitation system, if formally deferred
- Subclaims
- Fire-spread enforcement
- Mob-griefing enforcement
- Additional localization languages
- Optional debounced/asynchronous persistence for very high mutation volume
- Further BlueMap appearance configuration

---

# Testing Workflow

Every development batch must include:

1. Files to replace or delete
2. Build command
3. Thorough in-game test checklist
4. Next milestone

Build:

```powershell
.\gradlew.bat clean build
```

---

# Session Handoff

When starting a new chat, upload:

- `DEVELOPMENT.md`
- The current modified Java/resource files, or preferably a fresh current source ZIP

When the user says **"Create a prompt for a new chat"**:

1. Update `DEVELOPMENT.md` with all completed work.
2. Update the roadmap and release blockers.
3. Update the current milestone and completion estimate.
4. Generate a continuation prompt.
5. Treat the updated `DEVELOPMENT.md` as the new source of truth.
