package com.bananasandwich.bananaclaims.command.subowner;

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

public final class AddSubOwnerCommand {

    private AddSubOwnerCommand() {
    }

    public static RequiredArgumentBuilder<CommandSourceStack, String> localPlayerArgument() {
        return Commands.argument(
                        "player",
                        StringArgumentType.word()
                )
                .suggests(ClaimSuggestions.ONLINE_PLAYERS)
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
                .suggests(ClaimSuggestions.ONLINE_PLAYERS)
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
        ServerPlayer owner = source.getPlayerOrException();
        Optional<Claim> optionalClaim =
                ClaimResolver.findAtPlayer(owner);

        if (optionalClaim.isEmpty()) {
            source.sendFailure(
                    BananaClaimsMessages.text("command.bananaclaims.error.no_claim_here")
            );
            return 0;
        }

        return addSubOwner(
                source,
                owner,
                optionalClaim.get(),
                playerName
        );
    }

    private static int addToNamedClaim(
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

        return addSubOwner(
                source,
                owner,
                optionalClaim.get(),
                playerName
        );
    }

    private static int addSubOwner(
            CommandSourceStack source,
            ServerPlayer owner,
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
                Bananaclaims.CLAIM_MANAGER.addSubOwner(
                        claim,
                        owner.getUUID(),
                        target.getUUID(),
                        target.getName().getString()
                );

        return switch (result) {
            case SUBOWNER_ADDED, MEMBER_PROMOTED_TO_SUBOWNER -> {
                boolean promoted =
                        result == ClaimMutationResult.MEMBER_PROMOTED_TO_SUBOWNER;

                source.sendSuccess(
                        () -> BananaClaimsMessages.text("command.bananaclaims.subowner.add.success_actor", BananaClaimsMessages.string(promoted ? "command.bananaclaims.subowner.add.promoted_verb" : "command.bananaclaims.subowner.add.added_verb"), target.getName().getString(), claim.getName()),
                        false
                );

                target.sendSystemMessage(
                        BananaClaimsMessages.text("command.bananaclaims.subowner.add.success_target", claim.getName())
                );

                yield 1;
            }

            case PLAYER_IS_OWNER -> {
                source.sendFailure(
                        BananaClaimsMessages.text("command.bananaclaims.subowner.add.owner", target.getName().getString())
                );
                yield 0;
            }

            case PLAYER_IS_SUBOWNER -> {
                source.sendFailure(
                        BananaClaimsMessages.text("command.bananaclaims.subowner.add.exists", target.getName().getString(), claim.getName())
                );
                yield 0;
            }

            case NOT_AUTHORIZED -> {
                source.sendFailure(
                        BananaClaimsMessages.text("command.bananaclaims.subowner.add.not_authorized")
                );
                yield 0;
            }

            default -> {
                source.sendFailure(
                        BananaClaimsMessages.text("command.bananaclaims.subowner.add.failed")
                );
                yield 0;
            }
        };
    }
}
