# Banana Claims Permissions

Banana Claims integrates with LuckPerms through Fabric Permissions API v0 when available. If the external provider is unavailable or disabled, configurable vanilla command-level fallbacks are used.

A permission controls whether a command is available. Claim ownership and role checks still apply after permission approval.

## Public Commands

```text
bananaclaims.command.claim
bananaclaims.command.pos1
bananaclaims.command.pos2
bananaclaims.command.create
bananaclaims.command.createarea
bananaclaims.command.preview
bananaclaims.command.leave
bananaclaims.command.info
bananaclaims.command.list
```

## Claim Management

```text
bananaclaims.command.expand
bananaclaims.command.shrink
bananaclaims.command.delete
bananaclaims.command.rename
bananaclaims.command.description
bananaclaims.command.member
bananaclaims.command.subowner
bananaclaims.command.transfer
bananaclaims.command.flag
bananaclaims.command.popup
```

## Administration

The broad node grants all Banana Claims administrative commands:

```text
bananaclaims.command.admin
```

Granular nodes:

```text
bananaclaims.command.admin.list
bananaclaims.command.admin.info
bananaclaims.command.admin.nearest
bananaclaims.command.admin.force-transfer
bananaclaims.command.admin.force-delete
bananaclaims.command.admin.reload
bananaclaims.command.admin.reload.config
bananaclaims.command.admin.reload.claims
bananaclaims.command.admin.reload.preview
bananaclaims.command.admin.diagnostics
```

Granting one granular node exposes only the relevant administration branch. Granting `bananaclaims.command.admin` exposes all branches.

## Protection Bypass

```text
bananaclaims.protection.bypass
```

This bypasses enabled protection flags globally. Owners, subowners, and members already have role-based access and do not need this node for their own claims.

## Vanilla Fallbacks

`config/bananaclaims.json` contains:

```text
publicFallbackLevel
managementFallbackLevel
adminFallbackLevel
fallbackLevelOverrides
```

Levels are clamped to `0` through `4`. Defaults preserve public player commands and require administrator level 3 for administrative commands and global protection bypass.

Example exact-node override:

```json
"fallbackLevelOverrides": {
  "bananaclaims.command.flag": 2,
  "bananaclaims.protection.bypass": 4
}
```

## LuckPerms Examples

Grant all administrative functionality to a staff group:

```text
/lp group admin permission set bananaclaims.command.admin true
```

Grant diagnostics only:

```text
/lp group moderator permission set bananaclaims.command.admin.diagnostics true
```

Explicitly deny claim deletion:

```text
/lp group default permission set bananaclaims.command.delete false
```
