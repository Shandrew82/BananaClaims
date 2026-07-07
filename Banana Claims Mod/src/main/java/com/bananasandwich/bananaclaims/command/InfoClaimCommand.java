package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.Optional;

public class InfoClaimCommand {

    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("info")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ChunkPos chunkPos = player.chunkPosition();
                    String dimension = player.level().dimension().toString();

                    Optional<Claim> optionalClaim = Bananaclaims.CLAIM_MANAGER.getClaimAt(
                            dimension,
                            chunkPos.x(),
                            chunkPos.z()
                    );

                    if (optionalClaim.isEmpty()) {
                        context.getSource().sendSuccess(
                                () -> Component.literal("You are standing in Wilderness."),
                                false
                        );
                        return 1;
                    }

                    Claim claim = optionalClaim.get();

                    context.getSource().sendSuccess(
                            () -> Component.literal(
                                    "Claim: " + claim.getName()
                                            + "\nOwner: " + claim.getOwnerName()
                                            + "\nDescription: " + (claim.getDescription().isBlank() ? "No description." : claim.getDescription())
                                            + "\nDimension: " + claim.getDimension()
                                            + "\nChunk: " + claim.getChunkX() + ", " + claim.getChunkZ()
                            ),
                            false
                    );

                    return 1;
                });
    }
}