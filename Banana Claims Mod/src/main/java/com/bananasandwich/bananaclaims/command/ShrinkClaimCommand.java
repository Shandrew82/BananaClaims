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

public class ShrinkClaimCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("shrink")
                .then(Commands.argument(
                                        "claim",
                                        StringArgumentType.word()
                                )
                                .suggests(ClaimSuggestions.MANAGED_CLAIMS)
                                .executes(context -> shrinkClaim(
                                        context.getSource(),
                                        StringArgumentType.getString(
                                                context,
                                                "claim"
                                        )
                                ))
                );
    }

    private static int shrinkClaim(
            CommandSourceStack source,
            String claimName
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

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

        ChunkPos chunkPosition = player.chunkPosition();
        String dimension = player.level().dimension().toString();

        if (!claim.containsChunk(
                dimension,
                chunkPosition.x(),
                chunkPosition.z()
        )) {
            source.sendFailure(
                    Component.literal(
                            "This chunk is not part of claim \""
                                    + claim.getName()
                                    + "\"."
                    )
            );

            return 0;
        }

        if (claim.getChunks().size() <= 1) {
            source.sendFailure(
                    Component.literal(
                            "You cannot shrink the last chunk of a claim. "
                                    + "Use /claim delete instead."
                    )
            );

            return 0;
        }

        claim.removeChunk(
                dimension,
                chunkPosition.x(),
                chunkPosition.z()
        );

        Bananaclaims.CLAIM_MANAGER.saveClaims();

        source.sendSuccess(
                () -> Component.literal(
                        "Removed this chunk from claim \""
                                + claim.getName()
                                + "\"."
                ),
                false
        );

        return 1;
    }
}