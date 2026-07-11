package com.bananasandwich.bananaclaims.protection;

import com.bananasandwich.bananaclaims.claim.ClaimFlags;
import com.bananasandwich.bananaclaims.localization.BananaClaimsMessages;
import net.minecraft.network.chat.Component;

/**
 * A concrete protected action performed inside a claim.
 *
 * <p>Several actions intentionally share one persisted claim flag. Keeping
 * the actions separate gives callers precise denial messages while preserving
 * the compact flag model and JSON compatibility.</p>
 */
public enum ProtectionAction {

    BREAK_BLOCKS(
            "protection.bananaclaims.break_blocks"
    ),

    PLACE_BLOCKS(
            "protection.bananaclaims.place_blocks"
    ),

    BLOCK_INTERACTION(
            "protection.bananaclaims.block_interaction"
    ),

    CONTAINER_INTERACTION(
            "protection.bananaclaims.container_interaction"
    ),

    ENTITY_INTERACTION(
            "protection.bananaclaims.entity_interaction"
    ),

    ENTITY_PLACEMENT(
            "protection.bananaclaims.entity_placement"
    ),

    ENTITY_DAMAGE(
            "protection.bananaclaims.entity_damage"
    ),

    PVP(
            "protection.bananaclaims.pvp"
    ),

    EXPLOSION(
            "protection.bananaclaims.explosion"
    );

    private final String translationKey;

    ProtectionAction(
            String translationKey
    ) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public Component getDenialComponent() {
        return BananaClaimsMessages.text(
                translationKey
        );
    }

    public boolean isEnabled(
            ClaimFlags flags
    ) {
        if (flags == null) {
            return false;
        }

        return switch (this) {
            case BREAK_BLOCKS -> flags.isBreakBlocks();
            case PLACE_BLOCKS -> flags.isPlaceBlocks();
            case BLOCK_INTERACTION -> flags.isInteract();
            case CONTAINER_INTERACTION -> flags.isContainers();
            case ENTITY_INTERACTION,
                 ENTITY_PLACEMENT,
                 ENTITY_DAMAGE -> flags.isEntities();
            case PVP -> flags.isPvp();
            case EXPLOSION -> flags.isExplosions();
        };
    }
}
