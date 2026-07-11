# Banana Claims Protection Flags

All protection flags default to `false`. A new claim remains open to outsiders until its owner or an authorized manager enables protection.

Use:

```text
/claim flag <claim> <flag> <true|false>
```

## Active Flags

| Canonical name | Common aliases | Effect when enabled |
|---|---|---|
| `breakblocks` | `blockbreak`, `break` | Prevents outsiders from breaking blocks. |
| `placeblocks` | `blockplace`, `place` | Prevents outsiders from placing blocks. |
| `interact` | `interaction`, `interactions` | Protects doors, trapdoors, gates, switches, pressure plates, tools, buckets, and utility block interactions. |
| `containers` | `container` | Protects block containers, chest boats, and container minecarts. |
| `entities` | `entity` | Protects entity interaction, entity placement, and player-attributed entity damage. |
| `pvp` | `playerdamage` | Prevents outsider-versus-player damage when the victim is inside the claim. |
| `explosions` | `explosion` | Protects claim blocks, players, and entities from explosions. |

## Access Bypass

Enabled protections do not block:

- Claim owners
- Claim subowners
- Claim members
- Players with `bananaclaims.protection.bypass`

## Flag Independence

Flags are evaluated independently. For example, `containers=true` can protect chests while `interact=false` still permits doors and buttons.

## Reserved Data Fields

`fireSpread` and `mobGriefing` remain in persisted claim data for future compatibility but are not exposed through `/claim flag` because they do not yet have production enforcement hooks.
