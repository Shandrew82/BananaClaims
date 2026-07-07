package com.bananasandwich.bananaclaims.notification;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ClaimNotificationManager {
    private static final Map<UUID, LastKnownClaim> LAST_KNOWN_CLAIMS = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                checkPlayerClaimLocation(player);
            }
        });
    }

    private static void checkPlayerClaimLocation(ServerPlayer player) {
        UUID playerUuid = player.getUUID();

        String dimension = player.level().dimension().toString();
        ChunkPos chunkPos = player.chunkPosition();

        Optional<Claim> currentClaim = Bananaclaims.CLAIM_MANAGER.getClaimAt(
                dimension,
                chunkPos.x(),
                chunkPos.z()
        );

        LastKnownClaim currentKnownClaim = currentClaim
                .map(ClaimNotificationManager::toLastKnownClaim)
                .orElse(null);

        LastKnownClaim previousKnownClaim = LAST_KNOWN_CLAIMS.get(playerUuid);

        if (sameClaim(previousKnownClaim, currentKnownClaim)) {
            return;
        }

        if (previousKnownClaim != null) {
            player.sendSystemMessage(
                    Component.literal("Leaving claim: " + previousKnownClaim.name),
                    true
            );
        }

        if (currentKnownClaim != null) {
            player.sendSystemMessage(
                    Component.literal("Entering claim: " + currentKnownClaim.name),
                    true
            );

            LAST_KNOWN_CLAIMS.put(playerUuid, currentKnownClaim);
        } else {
            LAST_KNOWN_CLAIMS.remove(playerUuid);
        }
    }

    private static LastKnownClaim toLastKnownClaim(Claim claim) {
        return new LastKnownClaim(
                claim.getOwnerUuid() + "|" + claim.getName(),
                claim.getName()
        );
    }

    private static boolean sameClaim(LastKnownClaim first, LastKnownClaim second) {
        if (first == null && second == null) {
            return true;
        }

        if (first == null || second == null) {
            return false;
        }

        return first.key.equals(second.key);
    }

    private static class LastKnownClaim {
        private final String key;
        private final String name;

        private LastKnownClaim(String key, String name) {
            this.key = key;
            this.name = name;
        }
    }
}