package com.bananasandwich.bananaclaims.claim;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Canonical registry of every persisted claim flag.
 *
 * <p>Commands, tab completion, diagnostics, and the future Book GUI should
 * all use this registry rather than duplicating flag-name switch statements.</p>
 */
public enum ClaimFlagDefinition {

    BREAK_BLOCKS(
            "breakblocks",
            "Prevents outsiders from breaking blocks.",
            "blockbreak",
            "breakblocks",
            "break"
    ),

    PLACE_BLOCKS(
            "placeblocks",
            "Prevents outsiders from placing blocks.",
            "blockplace",
            "placeblocks",
            "place"
    ),

    INTERACT(
            "interact",
            "Protects doors, switches, pressure plates, tools, and utility interactions.",
            "interaction",
            "interactions"
    ),

    CONTAINERS(
            "containers",
            "Protects block and vehicle containers.",
            "container"
    ),

    ENTITIES(
            "entities",
            "Protects entity interaction, placement, and player-caused damage.",
            "entity"
    ),

    PVP(
            "pvp",
            "Prevents outsider-versus-player damage inside the claim.",
            "playerdamage"
    ),

    EXPLOSIONS(
            "explosions",
            "Protects claim blocks and entities from explosions.",
            "explosion"
    ),

    FIRE_SPREAD(
            "firespread",
            "Reserved fire-spread protection flag.",
            "fire",
            "fire-spread"
    ),

    MOB_GRIEFING(
            "mobgriefing",
            "Reserved mob-griefing protection flag.",
            "mobgrief",
            "mob-griefing"
    );

    private final String canonicalName;
    private final String description;
    private final List<String> aliases;

    ClaimFlagDefinition(
            String canonicalName,
            String description,
            String... aliases
    ) {
        this.canonicalName = canonicalName;
        this.description = description;
        this.aliases = Arrays.stream(aliases)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public String getDescription() {
        return description;
    }

    public boolean getValue(ClaimFlags flags) {
        if (flags == null) {
            return false;
        }

        return switch (this) {
            case BREAK_BLOCKS -> flags.isBreakBlocks();
            case PLACE_BLOCKS -> flags.isPlaceBlocks();
            case INTERACT -> flags.isInteract();
            case CONTAINERS -> flags.isContainers();
            case ENTITIES -> flags.isEntities();
            case PVP -> flags.isPvp();
            case EXPLOSIONS -> flags.isExplosions();
            case FIRE_SPREAD -> flags.isFireSpread();
            case MOB_GRIEFING -> flags.isMobGriefing();
        };
    }

    public void setValue(
            ClaimFlags flags,
            boolean value
    ) {
        if (flags == null) {
            return;
        }

        switch (this) {
            case BREAK_BLOCKS -> flags.setBreakBlocks(value);
            case PLACE_BLOCKS -> flags.setPlaceBlocks(value);
            case INTERACT -> flags.setInteract(value);
            case CONTAINERS -> flags.setContainers(value);
            case ENTITIES -> flags.setEntities(value);
            case PVP -> flags.setPvp(value);
            case EXPLOSIONS -> flags.setExplosions(value);
            case FIRE_SPREAD -> flags.setFireSpread(value);
            case MOB_GRIEFING -> flags.setMobGriefing(value);
        }
    }

    public boolean isCommandAvailable() {
        return this != FIRE_SPREAD
                && this != MOB_GRIEFING;
    }

    public static Optional<ClaimFlagDefinition> findCommandFlag(
            String input
    ) {
        return find(input)
                .filter(ClaimFlagDefinition::isCommandAvailable);
    }

    public static Optional<ClaimFlagDefinition> find(
            String input
    ) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        String normalized = input
                .trim()
                .toLowerCase(Locale.ROOT);

        return Arrays.stream(values())
                .filter(flag ->
                        flag.canonicalName.equals(normalized)
                                || flag.aliases.contains(normalized)
                )
                .findFirst();
    }

    public static List<String> canonicalNames() {
        return Arrays.stream(values())
                .filter(ClaimFlagDefinition::isCommandAvailable)
                .map(ClaimFlagDefinition::getCanonicalName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
