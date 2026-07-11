package com.bananasandwich.bananaclaims.command.subowner;

import com.bananasandwich.bananaclaims.Bananaclaims;
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
import net.minecraft.network.chat.Component;
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
                    Component.literal(
                            "There is no claim here."
                    )
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
                    Component.literal(
                            "You do not own a claim named \""
                                    + claimName
                                    + "\"."
                    )
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
                    Component.literal(
                            "\""
                                    + playerName
                                    + "\" is not a subowner of claim \""
                                    + claim.getName()
                                    + "\"."
                    )
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
                    Component.literal(
                            result == ClaimMutationResult.NOT_AUTHORIZED
                                    ? "Only the claim owner can remove subowners."
                                    : "Unable to remove that subowner."
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Removed "
                                + subOwner.getName()
                                + " as a subowner of claim \""
                                + claim.getName()
                                + "\". They remain a regular member."
                ),
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
                                Component.literal(
                                        "You are no longer a subowner of claim \""
                                                + claim.getName()
                                                + "\". You remain a regular member."
                                )
                        )
                );

        return 1;
    }
}
