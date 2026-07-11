package com.bananasandwich.bananaclaims.command.member;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
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

public final class AddMemberCommand {

    private AddMemberCommand() {
    }

    public static RequiredArgumentBuilder<CommandSourceStack, String> localPlayerArgument() {
        return Commands.argument(
                        "player",
                        StringArgumentType.word()
                )
                .suggests(
                        ClaimSuggestions.ONLINE_PLAYERS
                )
                .executes(context ->
                        addToCurrentClaim(
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
                        ClaimSuggestions.ONLINE_PLAYERS
                )
                .executes(context ->
                        addToNamedClaim(
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

    private static int addToCurrentClaim(
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

        return addMember(
                source,
                actor,
                optionalClaim.get(),
                playerName
        );
    }

    private static int addToNamedClaim(
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

        return addMember(
                source,
                actor,
                optionalClaim.get(),
                playerName
        );
    }

    private static int addMember(
            CommandSourceStack source,
            ServerPlayer actor,
            Claim claim,
            String playerName
    ) {
        Optional<ServerPlayer> optionalTarget =
                source.getServer()
                        .getPlayerList()
                        .getPlayers()
                        .stream()
                        .filter(player ->
                                player.getName()
                                        .getString()
                                        .equalsIgnoreCase(playerName)
                        )
                        .findFirst();

        if (optionalTarget.isEmpty()) {
            source.sendFailure(
                    Component.literal(
                            "No online player found named \""
                                    + playerName
                                    + "\"."
                    )
            );

            return 0;
        }

        ServerPlayer target = optionalTarget.get();

        ClaimMutationResult result =
                Bananaclaims.CLAIM_MANAGER.addMember(
                        claim,
                        actor.getUUID(),
                        target.getUUID(),
                        target.getName().getString()
                );

        return switch (result) {
            case MEMBER_ADDED -> {
                source.sendSuccess(
                        () -> Component.literal(
                                "Added "
                                        + target.getName().getString()
                                        + " as a member of claim \""
                                        + claim.getName()
                                        + "\"."
                        ),
                        false
                );

                target.sendSystemMessage(
                        Component.literal(
                                "You were added as a member of claim \""
                                        + claim.getName()
                                        + "\" by "
                                        + actor.getName().getString()
                                        + "."
                        )
                );

                yield 1;
            }

            case PLAYER_IS_OWNER -> {
                source.sendFailure(
                        Component.literal(
                                target.getName().getString()
                                        + " already owns this claim."
                        )
                );

                yield 0;
            }

            case PLAYER_IS_SUBOWNER -> {
                source.sendFailure(
                        Component.literal(
                                target.getName().getString()
                                        + " is already a subowner of claim \""
                                        + claim.getName()
                                        + "\"."
                        )
                );

                yield 0;
            }

            case PLAYER_IS_MEMBER -> {
                source.sendFailure(
                        Component.literal(
                                target.getName().getString()
                                        + " is already a member of claim \""
                                        + claim.getName()
                                        + "\"."
                        )
                );

                yield 0;
            }

            case NOT_AUTHORIZED -> {
                source.sendFailure(
                        Component.literal(
                                "You cannot edit members for this claim."
                        )
                );

                yield 0;
            }

            default -> {
                source.sendFailure(
                        Component.literal(
                                "Unable to add that player as a member."
                        )
                );

                yield 0;
            }
        };
    }
}
