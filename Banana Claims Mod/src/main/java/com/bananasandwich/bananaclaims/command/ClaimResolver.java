package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.Optional;
import java.util.UUID;

public final class ClaimResolver {

    private ClaimResolver() {
    }

    public static Optional<Claim> findByName(String claimName) {
        if (claimName == null || claimName.isBlank()) {
            return Optional.empty();
        }

        return Bananaclaims.CLAIM_MANAGER.getAllClaims()
                .stream()
                .filter(claim ->
                        claim.getName() != null
                                && claim.getName().equalsIgnoreCase(claimName)
                )
                .findFirst();
    }

    public static Optional<Claim> findOwnedByName(
            UUID ownerUuid,
            String claimName
    ) {
        if (ownerUuid == null
                || claimName == null
                || claimName.isBlank()) {
            return Optional.empty();
        }

        return Bananaclaims.CLAIM_MANAGER
                .getClaimsForOwner(ownerUuid)
                .stream()
                .filter(claim ->
                        claim.getName() != null
                                && claim.getName().equalsIgnoreCase(claimName)
                )
                .findFirst();
    }

    public static Optional<Claim> findManagedByName(
            UUID playerUuid,
            String claimName
    ) {
        if (playerUuid == null
                || claimName == null
                || claimName.isBlank()) {
            return Optional.empty();
        }

        return Bananaclaims.CLAIM_MANAGER
                .getAllClaims()
                .stream()
                .filter(claim ->
                        claim.canManage(playerUuid)
                                && claim.getName() != null
                                && claim.getName().equalsIgnoreCase(claimName)
                )
                .findFirst();
    }

    public static Optional<Claim> findParticipatingByName(
            UUID playerUuid,
            String claimName
    ) {
        if (playerUuid == null
                || claimName == null
                || claimName.isBlank()) {
            return Optional.empty();
        }

        return Bananaclaims.CLAIM_MANAGER
                .getAllClaims()
                .stream()
                .filter(claim ->
                        claim.hasAccess(playerUuid)
                                && claim.getName() != null
                                && claim.getName().equalsIgnoreCase(claimName)
                )
                .findFirst();
    }

    public static Optional<Claim> findAtPlayer(
            ServerPlayer player
    ) {
        if (player == null) {
            return Optional.empty();
        }

        ChunkPos chunkPosition = player.chunkPosition();
        String dimension =
                player.level().dimension().toString();

        return Bananaclaims.CLAIM_MANAGER.getClaimAt(
                dimension,
                chunkPosition.x(),
                chunkPosition.z()
        );
    }

    public static Optional<Claim> findAtSource(
            CommandSourceStack source
    ) {
        try {
            return findAtPlayer(
                    source.getPlayerOrException()
            );
        } catch (Exception exception) {
            return Optional.empty();
        }
    }
}
