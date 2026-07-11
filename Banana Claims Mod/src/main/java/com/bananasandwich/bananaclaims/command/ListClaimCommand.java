package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.localization.BananaClaimsMessages;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class ListClaimCommand {

    private ListClaimCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("list")
                .executes(context -> {
                    ServerPlayer player =
                            context.getSource()
                                    .getPlayerOrException();

                    List<Claim> claims =
                            Bananaclaims.CLAIM_MANAGER
                                    .getClaimsForOwner(
                                            player.getUUID()
                                    );

                    if (claims.isEmpty()) {
                        context.getSource().sendSuccess(
                                () -> BananaClaimsMessages.text(
                                        "command.bananaclaims.list.empty"
                                ),
                                false
                        );
                        return 1;
                    }

                    StringBuilder message = new StringBuilder(
                            BananaClaimsMessages.string(
                                    "command.bananaclaims.list.header",
                                    claims.size()
                            )
                    );

                    for (Claim claim : claims) {
                        message.append(
                                BananaClaimsMessages.string(
                                        "command.bananaclaims.list.entry",
                                        claim.getName(),
                                        claim.getDimension(),
                                        claim.getChunks().size()
                                )
                        );
                    }

                    context.getSource().sendSuccess(
                            () -> BananaClaimsMessages.text(
                                    "command.bananaclaims.raw",
                                    message
                            ),
                            false
                    );

                    return 1;
                });
    }
}
