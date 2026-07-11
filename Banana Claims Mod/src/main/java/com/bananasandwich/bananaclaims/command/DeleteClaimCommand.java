package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.localization.BananaClaimsMessages;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class DeleteClaimCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("delete")
                .executes(context ->
                        deleteCurrentClaim(
                                context.getSource()
                        )
                )
                .then(Commands.argument(
                                        "claim",
                                        StringArgumentType.word()
                                )
                                .suggests(
                                        ClaimSuggestions.OWNED_CLAIMS
                                )
                                .executes(context ->
                                        deleteNamedClaim(
                                                context.getSource(),
                                                StringArgumentType.getString(
                                                        context,
                                                        "claim"
                                                )
                                        )
                                )
                );
    }

    private static int deleteCurrentClaim(
            CommandSourceStack source
    ) throws CommandSyntaxException {
        ServerPlayer player =
                source.getPlayerOrException();

        Optional<Claim> optionalClaim =
                ClaimResolver.findAtPlayer(player);

        if (optionalClaim.isEmpty()) {
            source.sendFailure(
                    BananaClaimsMessages.text("command.bananaclaims.error.no_claim_here")
            );

            return 0;
        }

        Claim claim = optionalClaim.get();

        if (!claim.isOwner(player.getUUID())) {
            source.sendFailure(
                    BananaClaimsMessages.text("command.bananaclaims.error.not_owner")
            );

            return 0;
        }

        return deleteClaim(
                source,
                claim
        );
    }

    private static int deleteNamedClaim(
            CommandSourceStack source,
            String claimName
    ) throws CommandSyntaxException {
        ServerPlayer player =
                source.getPlayerOrException();

        Optional<Claim> optionalClaim =
                ClaimResolver.findOwnedByName(
                        player.getUUID(),
                        claimName
                );

        if (optionalClaim.isEmpty()) {
            source.sendFailure(
                    BananaClaimsMessages.text("command.bananaclaims.error.not_owner_named", claimName)
            );

            return 0;
        }

        return deleteClaim(
                source,
                optionalClaim.get()
        );
    }

    private static int deleteClaim(
            CommandSourceStack source,
            Claim claim
    ) {
        String claimName = claim.getName();

        boolean removed =
                Bananaclaims.CLAIM_MANAGER.removeClaim(
                        claim.getDimension(),
                        claim.getChunkX(),
                        claim.getChunkZ()
                );

        if (!removed) {
            source.sendFailure(
                    BananaClaimsMessages.text("command.bananaclaims.delete.failed", claimName)
            );

            return 0;
        }

        source.sendSuccess(
                () -> BananaClaimsMessages.text("command.bananaclaims.delete.success", claimName),
                false
        );

        return 1;
    }
}