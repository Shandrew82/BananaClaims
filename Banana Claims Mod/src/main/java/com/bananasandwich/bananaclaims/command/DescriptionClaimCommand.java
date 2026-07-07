package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.Optional;

public class DescriptionClaimCommand {

    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("description")
                .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ChunkPos chunkPos = player.chunkPosition();
                            String dimension = player.level().dimension().toString();
                            String description = StringArgumentType.getString(context, "text");

                            Optional<Claim> optionalClaim = Bananaclaims.CLAIM_MANAGER.getClaimAt(
                                    dimension,
                                    chunkPos.x(),
                                    chunkPos.z()
                            );

                            if (optionalClaim.isEmpty()) {
                                context.getSource().sendFailure(
                                        Component.literal("There is no claim here.")
                                );
                                return 0;
                            }

                            Claim claim = optionalClaim.get();

                            if (!claim.isOwner(player.getUUID())) {
                                context.getSource().sendFailure(
                                        Component.literal("You do not own this claim.")
                                );
                                return 0;
                            }

                            claim.setDescription(description);
                            Bananaclaims.CLAIM_MANAGER.saveClaims();

                            context.getSource().sendSuccess(
                                    () -> Component.literal("Claim description updated."),
                                    false
                            );

                            return 1;
                        })
                );
    }
}