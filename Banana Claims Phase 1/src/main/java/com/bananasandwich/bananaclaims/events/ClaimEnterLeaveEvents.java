package com.bananasandwich.bananaclaims.events;

import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.claim.ClaimManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ClaimEnterLeaveEvents {
    private static final Map<UUID, String> LAST_CLAIM_ID = new HashMap<>();
    private static int tickCounter = 0;

    private ClaimEnterLeaveEvents() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter < 10) return;
            tickCounter = 0;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                Optional<Claim> current = ClaimManager.getClaimAt(player);
                String previousId = LAST_CLAIM_ID.get(player.getUUID());
                String currentId = current.map(claim -> claim.id).orElse(null);

                if (currentId == null && previousId != null) {
                    player.sendSystemMessage(Component.literal("Leaving claim"), true);
                    LAST_CLAIM_ID.remove(player.getUUID());
                } else if (currentId != null && !currentId.equals(previousId)) {
                    Claim claim = current.get();
                    player.sendSystemMessage(Component.literal("Entering " + claim.name), true);
                    LAST_CLAIM_ID.put(player.getUUID(), currentId);
                }
            }
        });
    }
}
