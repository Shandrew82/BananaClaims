package com.bananasandwich.bananaclaims.command.member;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.claim.ClaimMember;
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
        ServerPlayer owner =
                source.getPlayerOrException();

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

        Claim claim = optionalClaim.get();

        if (!claim.isOwner(owner.getUUID())) {
            source.sendFailure(
                    Component.literal(
                            "You do not own this claim."
                    )
            );

            return 0;
        }

        return removeMember(
                source,
                claim,
                playerName
        );
    }

    private static int removeFromNamedClaim(
            CommandSourceStack source,
            String claimName,
            String playerName
    ) throws CommandSyntaxException {
        ServerPlayer owner =
                source.getPlayerOrException();

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

        return removeMember(
                source,
                optionalClaim.get(),
                playerName
        );
    }

    private static int removeMember(
            CommandSourceStack source,
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
                                    + "\" is not a member of claim \""
                                    + claim.getName()
                                    + "\"."
                    )
            );

            return 0;
        }

        ClaimMember member = optionalMember.get();

        boolean removed =
                claim.removeMember(member.getUuid());

        if (!removed) {
            source.sendFailure(
                    Component.literal(
                            "Unable to remove that member."
                    )
            );

            return 0;
        }

        Bananaclaims.CLAIM_MANAGER.saveClaims();

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
                                                + "\"."
                                )
                        )
                );

        return 1;
    }
}