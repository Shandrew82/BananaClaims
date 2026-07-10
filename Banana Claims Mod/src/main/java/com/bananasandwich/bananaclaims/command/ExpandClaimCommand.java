package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.Optional;

public class ExpandClaimCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("expand")
                .then(Commands.argument(
                                        "claim",
                                        StringArgumentType.word()
                                )
                                .suggests(ClaimSuggestions.MANAGED_CLAIMS)
                                .executes(context -> expandClaim(
                                        context.getSource(),
                                        StringArgumentType.getString(
                                                context,
                                                "claim"
                                        )
                                ))
                );
    }

    private static int expandClaim(
            CommandSourceStack source,
            String claimName
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        ChunkPos chunkPosition = player.chunkPosition();
        String dimension = player.level().dimension().toString();

        if (Bananaclaims.CLAIM_MANAGER.isChunkClaimed(
                dimension,
                chunkPosition.x(),
                chunkPosition.z()
        )) {
            source.sendFailure(
                    Component.literal(
                            "This chunk is already claimed."
                    )
            );

            return 0;
        }

        Optional<Claim> optionalClaim =
                ClaimResolver.findManagedByName(
                        player.getUUID(),
                        claimName
                );

        if (optionalClaim.isEmpty()) {
            source.sendFailure(
                    Component.literal(
                            "You cannot manage a claim named \""
                                    + claimName
                                    + "\"."
                    )
            );

            return 0;
        }

        Claim claim = optionalClaim.get();

        if (!claim.canResize(player.getUUID())) {
            source.sendFailure(
                    Component.literal(
                            "You cannot resize this claim."
                    )
            );

            return 0;
        }

        claim.addChunk(
                dimension,
                chunkPosition.x(),
                chunkPosition.z()
        );

        Bananaclaims.CLAIM_MANAGER.saveClaims();

        source.sendSuccess(
                () -> Component.literal(
                        "Added this chunk to claim \""
                                + claim.getName()
                                + "\"."
                ),
                false
        );

        return 1;
    }
}