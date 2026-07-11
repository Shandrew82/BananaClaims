# Banana Claims Performance and Architecture

## Claim Lookup

Claims are indexed by dimension and chunk coordinate. Protection checks, notifications, and location-based commands use constant-time chunk lookups instead of scanning every claim.

## Notifications

Player claim-location checks run every 10 server ticks. A player is skipped when their dimension, chunk, and claim-manager revision are unchanged.

## Preview Renderer

Renderer V2:

- Sends block-display packets only to the requesting player
- Does not add display entities to the server world
- Does not persist previews to world data
- Removes or replaces sessions by player UUID
- Sends animation metadata only during active fade or pulse updates
- Uses configurable update intervals for packet-volume control

Large or highly contoured claims create more display definitions. Raise fade or pulse update intervals if unusually large previews cause client or network pressure.

## Protection

Protection hooks perform a direct chunk lookup and return immediately when:

- The location is unclaimed
- The relevant flag is disabled
- The responsible player has claim access
- The responsible player has the global bypass permission

No global protection scan or protection tick loop is used.

## BlueMap

BlueMap fingerprints each claim and updates only the changed claim for normal events. A full synchronization occurs only after a complete claim-data load or an event without a specific claim. Disconnected claim outlines are emitted as separate polygons under one listed claim entry.

## Persistence

Claim data is serialized synchronously on claim mutations. Writes use a temporary file and atomic replacement where supported. Invalid startup data stops initialization instead of silently loading an empty claim set. This is appropriate for the current expected server scale; future very high-volume mutation workloads could add a debounced save queue.

## Audit Findings

- No legacy particle renderer remains.
- No placeholder client renderer or example mixin remains.
- No client installation is required.
- Protection and previews use centralized services.
- Command flag names come from one canonical registry.
- Player-facing normal command messages are resolved from `en_us.json` on the server, preventing raw custom translation keys on vanilla clients.
