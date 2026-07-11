package com.bananasandwich.bananaclaims.command.subowner;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.localization.BananaClaimsMessages;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.claim.ClaimMutationResult;
import com.bananasandwich.bananaclaims.claim.ClaimSubOwner;
import com.bananasandwich.bananaclaims.command.ClaimResolver;
import com.bananasandwich.bananaclaims.command.ClaimSuggestions;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class RemoveSubOwnerCommand {

    private RemoveSubOwnerCommand() {
    }

    public static RequiredArgumentBuilder<CommandSourceStack, String> localPlayerArgument() {
        return Commands.argument(
                        "player",
                        StringArgumentType.word()
                )
                .suggests(ClaimSuggestions.CURRENT_CLAIM_SUBOWNERS)
                .executes(context ->
                        removeFromCurrentClaim(
                                context.getSource(),
                                StringArgumentType.getString(
                                        context,
                                        "player"
                                )
                        )
                );
    }

    public static RequiredArgumentBuilder<CommandSourceStack, String> remotePlayerArgument() {
        return Commands.argument(
                        "player",
                        StringArgumentType.word()
                )
                .suggests(ClaimSuggestions.NAMED_CLAIM_SUBOWNERS)
                .executes(context ->
                        removeFromNamedClaim(
                                context.getSource(),
                                StringArgumentType.getString(
                                        context,
                                        "claim"
                                ),
                                StringArgumentType.getString(
                                        context,
                                        "player"
                                )
                        )
                );
    }

    private static int removeFromCurrentClaim(
            CommandSourceStack source,
            String playerName
    ) throws CommandSyntaxException {
        ServerPlayer owner = source.getPlayerOrException();
        Optional<Claim> optionalClaim =
                ClaimResolver.findAtPlayer(owner);

        if (optionalClaim.isEmpty()) {
            source.sendFailure(
                    BananaClaimsMessages.text("command.bananaclaims.error.no_claim_here")
            );
            return 0;
        }

        return removeSubOwner(
                source,
                owner,
                optionalClaim.get(),
                playerName
        );
    }

    private static int removeFromNamedClaim(
            CommandSourceStack source,
            String claimName,
            String playerName
    ) throws CommandSyntaxException {
        ServerPlayer owner = source.getPlayerOrException();
        Optional<Claim> optionalClaim =
                ClaimResolver.findOwnedByName(
                        owner.getUUID(),
                        claimName
                );

        if (optionalClaim.isEmpty()) {
            source.sendFailure(
                    BananaClaimsMessages.text("command.bananaclaims.error.not_owner_named", claimName)
            );
            return 0;
        }

        return removeSubOwner(
                source,
                owner,
                optionalClaim.get(),
                playerName
        );
    }

    private static int removeSubOwner(
            CommandSourceStack source,
            ServerPlayer owner,
            Claim claim,
            String playerName
    ) {
        Optional<ClaimSubOwner> optionalSubOwner =
                claim.getSubOwners()
                        .stream()
                        .filter(subOwner ->
                                subOwner.getName()
                                        .equalsIgnoreCase(playerName)
                        )
                        .findFirst();

        if (optionalSubOwner.isEmpty()) {
            source.sendFailure(
                    BananaClaimsMessages.text("command.bananaclaims.subowner.remove.not_subowner", playerName, claim.getName())
            );
            return 0;
        }

        ClaimSubOwner subOwner = optionalSubOwner.get();

        ClaimMutationResult result =
                Bananaclaims.CLAIM_MANAGER.demoteSubOwner(
                        claim,
                        owner.getUUID(),
                        subOwner.getUuid()
                );

        if (result != ClaimMutationResult.SUBOWNER_DEMOTED_TO_MEMBER) {
            source.sendFailure(
                    (result == ClaimMutationResult.NOT_AUTHORIZED ? BananaClaimsMessages.text("command.bananaclaims.subowner.remove.not_authorized") : BananaClaimsMessages.text("command.bananaclaims.subowner.remove.failed"))
            );
            return 0;
        }

        source.sendSuccess(
                () -> BananaClaimsMessages.text("command.bananaclaims.subowner.remove.success_actor", subOwner.getName(), claim.getName()),
                false
        );

        source.getServer()
                .getPlayerList()
                .getPlayers()
                .stream()
                .filter(player ->
                        player.getUUID().equals(subOwner.getUuid())
                )
                .findFirst()
                .ifPresent(player ->
                        player.sendSystemMessage(
                                BananaClaimsMessages.text("command.bananaclaims.subowner.remove.success_target", claim.getName())
                        )
                );

        return 1;
    }
}
