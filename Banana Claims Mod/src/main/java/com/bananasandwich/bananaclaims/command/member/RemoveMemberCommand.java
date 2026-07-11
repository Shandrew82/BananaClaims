package com.bananasandwich.bananaclaims.command.member;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.claim.ClaimMember;
import com.bananasandwich.bananaclaims.claim.ClaimMutationResult;
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

public final class RemoveMemberCommand {

    private RemoveMemberCommand() {
    }

    public static RequiredArgumentBuilder<CommandSourceStack, String> localPlayerArgument() {
        return Commands.argument(
                        "player",
                        StringArgumentType.word()
                )
                .suggests(
                        ClaimSuggestions.CURRENT_CLAIM_MEMBERS
                )
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
                .suggests(
                        ClaimSuggestions.NAMED_CLAIM_MEMBERS
                )
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
        ServerPlayer actor =
                source.getPlayerOrException();

        Optional<Claim> optionalClaim =
                ClaimResolver.findAtPlayer(actor);

        if (optionalClaim.isEmpty()) {
            source.sendFailure(
                    Component.literal(
                            "There is no claim here."
                    )
            );

            return 0;
        }

        return removeMember(
                source,
                actor,
                optionalClaim.get(),
                playerName
        );
    }

    private static int removeFromNamedClaim(
            CommandSourceStack source,
            String claimName,
            String playerName
    ) throws CommandSyntaxException {
        ServerPlayer actor =
                source.getPlayerOrException();

        Optional<Claim> optionalClaim =
                ClaimResolver.findManagedByName(
                        actor.getUUID(),
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

        return removeMember(
                source,
                actor,
                optionalClaim.get(),
                playerName
        );
    }

    private static int removeMember(
            CommandSourceStack source,
            ServerPlayer actor,
            Claim claim,
            String playerName
    ) {
        Optional<ClaimMember> optionalMember =
                claim.getMembers()
                        .stream()
                        .filter(member ->
                                member.getName()
                                        .equalsIgnoreCase(playerName)
                        )
                        .findFirst();

        if (optionalMember.isEmpty()) {
            source.sendFailure(
                    Component.literal(
                            "\""
                                    + playerName
                                    + "\" is not a regular member of claim \""
                                    + claim.getName()
                                    + "\"."
                    )
            );

            return 0;
        }

        ClaimMember member = optionalMember.get();

        ClaimMutationResult result =
                Bananaclaims.CLAIM_MANAGER.removeMember(
                        claim,
                        actor.getUUID(),
                        member.getUuid()
                );

        if (result != ClaimMutationResult.MEMBER_REMOVED) {
            source.sendFailure(
                    Component.literal(
                            result == ClaimMutationResult.NOT_AUTHORIZED
                                    ? "You cannot edit members for this claim."
                                    : "Unable to remove that member."
                    )
            );

            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Removed "
                                + member.getName()
                                + " from claim \""
                                + claim.getName()
                                + "\"."
                ),
                false
        );

        source.getServer()
                .getPlayerList()
                .getPlayers()
                .stream()
                .filter(player ->
                        player.getUUID()
                                .equals(member.getUuid())
                )
                .findFirst()
                .ifPresent(player ->
                        player.sendSystemMessage(
                                Component.literal(
                                        "You were removed from claim \""
                                                + claim.getName()
                                                + "\" by "
                                                + actor.getName().getString()
                                                + "."
                                )
                        )
                );

        return 1;
    }
}
