package com.bananasandwich.bananaclaims.notification;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.claim.ClaimPopupSettings;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ClaimNotificationManager {
    private static final Map<UUID, LastKnownClaim> LAST_KNOWN_CLAIMS = new HashMap<>();
    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;

            if (tickCounter < 10) {
                return;
            }

            tickCounter = 0;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                checkPlayerClaimLocation(player);
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            LAST_KNOWN_CLAIMS.remove(handler.player.getUUID());
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
            sendLeaveMessage(player, previousKnownClaim);
        }

        if (currentKnownClaim != null) {
            sendEnterMessage(player, currentKnownClaim);
            LAST_KNOWN_CLAIMS.put(playerUuid, currentKnownClaim);
        } else {
            LAST_KNOWN_CLAIMS.remove(playerUuid);
        }
    }

    private static void sendEnterMessage(ServerPlayer player, LastKnownClaim knownClaim) {
        String message = knownClaim.enterTitle;

        if (message == null || message.isBlank()) {
            message = "Entering " + knownClaim.name;
        }

        player.sendSystemMessage(Component.literal(message), true);
    }

    private static void sendLeaveMessage(ServerPlayer player, LastKnownClaim knownClaim) {
        String message = knownClaim.leaveTitle;

        if (message == null || message.isBlank()) {
            message = "Leaving " + knownClaim.name;
        }

        player.sendSystemMessage(Component.literal(message), true);
    }

    private static LastKnownClaim toLastKnownClaim(Claim claim) {
        ClaimPopupSettings popupSettings = claim.getPopupSettings();

        return new LastKnownClaim(
                claim.getOwnerUuid() + "|" + claim.getName(),
                claim.getName(),
                popupSettings.getEnterTitle(),
                popupSettings.getLeaveTitle()
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
        private final String enterTitle;
        private final String leaveTitle;

        private LastKnownClaim(String key, String name, String enterTitle, String leaveTitle) {
            this.key = key;
            this.name = name;
            this.enterTitle = enterTitle;
            this.leaveTitle = leaveTitle;
        }
    }
}