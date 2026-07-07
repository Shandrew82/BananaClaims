package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class ListClaimCommand {

    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("list")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();

                    List<Claim> claims = Bananaclaims.CLAIM_MANAGER.getClaimsForOwner(player.getUUID());

                    if (claims.isEmpty()) {
                        context.getSource().sendSuccess(
                                () -> Component.literal("You do not own any claims."),
                                false
                        );
                        return 1;
                    }

                    StringBuilder message = new StringBuilder("Your Claims:\n");

                    for (Claim claim : claims) {
                        message.append("- ")
                                .append(claim.getName())
                                .append(" (")
                                .append(claim.getChunkX())
                                .append(", ")
                                .append(claim.getChunkZ())
                                .append(")\n");
                    }

                    message.append("Total: ").append(claims.size());

                    context.getSource().sendSuccess(
                            () -> Component.literal(message.toString()),
                            false
                    );

                    return 1;
                });
    }
}