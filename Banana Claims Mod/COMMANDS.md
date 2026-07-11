# Banana Claims Commands

Arguments in `<angle brackets>` are required. Arguments in `[square brackets]` are optional. Tab completion is available for claim names, players, flags, booleans, sounds, and supported modes where applicable.

## Claims

| Command | Purpose |
|---|---|
| `/claim` | Confirms that Banana Claims is loaded. |
| `/claim create <name>` | Creates a single-chunk claim in the current chunk. |
| `/claim pos1` | Sets selection corner one at the player's position. |
| `/claim pos2` | Sets selection corner two and previews the selection when valid. |
| `/claim createarea <name>` | Creates a rectangular multi-chunk claim from the selection. |
| `/claim info [claim]` | Shows claim owner, roles, description, and chunk count. |
| `/claim list` | Lists claims owned by the player. |
| `/claim description <text>` | Updates the claim at the player's position. |
| `/claim description <claim> <text>` | Updates a managed claim remotely. |
| `/claim rename <newName>` | Renames the claim at the player's position. |
| `/claim rename <claim> <newName>` | Renames a managed claim remotely. |
| `/claim delete` | Deletes the owned claim at the player's position. |
| `/claim delete <claim>` | Deletes an owned claim remotely. |
| `/claim expand <claim>` | Adds the current unclaimed chunk to a managed claim. |
| `/claim shrink <claim>` | Removes the current chunk from a managed claim. |
| `/claim leave [claim]` | Leaves a claim; subowners step down to member first. |
| `/claim transfer [claim] <player>` | Transfers ownership to an online player. |

## Preview

| Command | Purpose |
|---|---|
| `/claim preview` | Previews the claim at the player's position. |
| `/claim preview <claim>` | Previews a managed claim by name. |
| `/claim preview nearest` | Previews the nearest managed claim in the current dimension. |
| `/claim preview stop` | Immediately removes the player's active preview. |

Previews are packet-only, client-isolated, and do not create persistent world entities.

## Members

Local forms operate on the claim at the player's position. Remote forms include the claim name.

```text
/claim member add <player>
/claim member remove <player>
/claim member list
/claim member <claim> add <player>
/claim member <claim> remove <player>
/claim member <claim> list
```

Owners and subowners can manage regular members. Members bypass enabled claim protections.

## Subowners

```text
/claim subowner add <player>
/claim subowner remove <player>
/claim subowner list
/claim subowner <claim> add <player>
/claim subowner <claim> remove <player>
/claim subowner <claim> list
```

Only the owner can add or remove subowners. Removing a subowner demotes them to a regular member.

## Flags

```text
/claim flag <claim> <flag>
/claim flag <claim> <flag> <true|false>
```

The first form displays the current value. The second changes it. See [FLAGS.md](FLAGS.md).

## Popup Configuration

```text
/claim popup <claim> preview enter
/claim popup <claim> preview leave
/claim popup <claim> set mode <ACTIONBAR|TITLE|CHAT>
/claim popup <claim> set enterTitle <text>
/claim popup <claim> set enterSubtitle <text>
/claim popup <claim> set leaveTitle <text>
/claim popup <claim> set leaveSubtitle <text>
/claim popup <claim> set enterSound <sound|none>
/claim popup <claim> set leaveSound <sound|none>
```

Popup text supports Banana Claims placeholders and inline hex color formatting already implemented by the renderer.

## Administration

```text
/claim admin list [page]
/claim admin info [claim]
/claim admin nearest
/claim admin force-transfer <claim> <player>
/claim admin force-delete <claim> confirm
/claim admin reload
/claim admin reload config
/claim admin reload claims
/claim admin reload preview
/claim admin diagnostics [claim]
```

Claims with duplicate display names can be selected with the generated `name@owner` selector or their claim UUID.
