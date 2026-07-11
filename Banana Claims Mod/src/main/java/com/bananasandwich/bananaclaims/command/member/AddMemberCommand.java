package com.bananasandwich.bananaclaims.command.member;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.localization.BananaClaimsMessages;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.claim.ClaimMutationResult;
import com.bananasandwich.bananaclaims.command.ClaimResolver;
import com.bananasandwich.bananaclaims.command.ClaimSuggestions;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
                    BananaClaimsMessages.text("command.bananaclaims.error.no_claim_here")
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
                    BananaClaimsMessages.text("command.bananaclaims.error.cannot_manage_named", claimName)
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
                    BananaClaimsMessages.text("command.bananaclaims.error.online_player_not_found", playerName)
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
                        () -> BananaClaimsMessages.text("command.bananaclaims.member.add.success_actor", target.getName().getString(), claim.getName()),
                        false
                );

                target.sendSystemMessage(
                        BananaClaimsMessages.text("command.bananaclaims.member.add.success_target", claim.getName(), actor.getName().getString())
                );

                yield 1;
            }

            case PLAYER_IS_OWNER -> {
                source.sendFailure(
                        BananaClaimsMessages.text("command.bananaclaims.member.add.owner", target.getName().getString())
                );

                yield 0;
            }

            case PLAYER_IS_SUBOWNER -> {
                source.sendFailure(
                        BananaClaimsMessages.text("command.bananaclaims.member.add.subowner", target.getName().getString(), claim.getName())
                );

                yield 0;
            }

            case PLAYER_IS_MEMBER -> {
                source.sendFailure(
                        BananaClaimsMessages.text("command.bananaclaims.member.add.member", target.getName().getString(), claim.getName())
                );

                yield 0;
            }

            case NOT_AUTHORIZED -> {
                source.sendFailure(
                        BananaClaimsMessages.text("command.bananaclaims.member.add.not_authorized")
                );

                yield 0;
            }

            default -> {
                source.sendFailure(
                        BananaClaimsMessages.text("command.bananaclaims.member.add.failed")
                );

                yield 0;
            }
        };
    }
}
