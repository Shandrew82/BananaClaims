package com.bananasandwich.bananaclaims.protection;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.ChunkPos;

import java.util.Optional;

public class ClaimProtectionManager {

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register(
                (world, player, pos, state, blockEntity) -> {
                    Optional<Claim> optionalClaim = getClaimAt(
                            world.dimension().toString(),
                            pos.getX(),
                            pos.getZ()
                    );

                    if (optionalClaim.isEmpty()) {
                        return true;
                    }

                    Claim claim = optionalClaim.get();

                    if (!claim.getFlags().isBreakBlocks()) {
                        return true;
                    }

                    if (claim.hasAccess(player.getUUID())) {
                        return true;
                    }

                    player.sendSystemMessage(
                            Component.literal(
                                    "You cannot break blocks in this claim."
                            )
                    );

                    return false;
                }
        );

        UseBlockCallback.EVENT.register(
                (player, world, hand, hitResult) -> {
                    if (!(player.getItemInHand(hand).getItem()
                            instanceof BlockItem)) {
                        return InteractionResult.PASS;
                    }

                    Optional<Claim> optionalClaim = getClaimAt(
                            world.dimension().toString(),
                            hitResult.getBlockPos().getX(),
                            hitResult.getBlockPos().getZ()
                    );

                    if (optionalClaim.isEmpty()) {
                        return InteractionResult.PASS;
                    }

                    Claim claim = optionalClaim.get();

                    if (!claim.getFlags().isPlaceBlocks()) {
                        return InteractionResult.PASS;
                    }

                    if (claim.hasAccess(player.getUUID())) {
                        return InteractionResult.PASS;
                    }

                    player.sendSystemMessage(
                            Component.literal(
                                    "You cannot place blocks in this claim."
                            )
                    );

                    return InteractionResult.FAIL;
                }
        );
    }

    private static Optional<Claim> getClaimAt(
            String dimension,
            int blockX,
            int blockZ
    ) {
        ChunkPos chunkPosition = new ChunkPos(
                blockX >> 4,
                blockZ >> 4
        );

        return Bananaclaims.CLAIM_MANAGER.getClaimAt(
                dimension,
                chunkPosition.x(),
                chunkPosition.z()
        );
    }
}