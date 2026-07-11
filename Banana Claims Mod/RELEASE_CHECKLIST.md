# Banana Claims 1.0 Release Checklist

## Build

- [ ] Run `.\gradlew.bat clean build`
- [ ] Confirm no compiler warnings indicate missing mixin targets
- [ ] Confirm the release JAR contains `fabric.mod.json`, mixin config, language file, and icon
- [ ] Confirm Java 25 is used for the production build

## Fresh Installation

- [ ] Start with no Banana Claims config directory
- [ ] Confirm both configuration files are generated
- [ ] Confirm an empty claims file is handled safely
- [ ] Confirm the server starts without BlueMap or LuckPerms installed

## Upgrade Installation

- [ ] Back up `config/bananaclaims/`
- [ ] Start with existing claims and preview config
- [ ] Confirm claim IDs and roles migrate without duplicates
- [ ] Confirm preview config remains version 4
- [ ] Confirm BlueMap removes obsolete marker IDs and redraws claims

## Claims and Roles

- [ ] Create single-chunk and area claims
- [ ] Expand and shrink irregular claims
- [ ] Rename, describe, list, and delete claims
- [ ] Add and remove members
- [ ] Promote and demote subowners
- [ ] Test member and subowner leave behavior
- [ ] Transfer ownership and restart

## Protection

- [ ] Verify every active flag with an outsider
- [ ] Verify owner, subowner, member, and permission bypass
- [ ] Test normal and weighted pressure plates
- [ ] Test cross-boundary explosions
- [ ] Confirm new claims remain unprotected by default

## Preview

- [ ] Test old and new claims
- [ ] Test irregular and disconnected outlines
- [ ] Test selection preview
- [ ] Test stop, expiration, dimension change, and disconnect
- [ ] Test two-player visibility isolation
- [ ] Test fade and pulse enabled and disabled

## BlueMap

- [ ] Test without BlueMap installed
- [ ] Test startup with BlueMap installed
- [ ] Confirm claim creation, update, transfer, and deletion refresh markers
- [ ] Confirm HTML escaping for claim names and descriptions
- [ ] Confirm disconnected claim sections render

## Permissions and Administration

- [ ] Test external permission grants and explicit denies
- [ ] Test vanilla fallback levels
- [ ] Test granular admin nodes
- [ ] Test force transfer and confirmed force delete
- [ ] Test main, claims, and preview reloads
- [ ] Run global and claim diagnostics

## Release Decision

- [ ] Resolve or explicitly defer the missing invitation system noted in `DEVELOPMENT.md`
- [ ] Confirm documentation matches the shipped command tree
- [ ] Update version and changelog
- [ ] Tag the release commit
