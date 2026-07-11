package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.claim.ClaimMutationResult;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

public final class LeaveClaimCommand {

    private LeaveClaimCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("leave")
                .executes(context ->
                        leaveCurrentClaim(
                                context.getSource()
                        )
                )
                .then(
                        Commands.argument(
                                        "claim",
                                        StringArgumentType.word()
                                )
                                .suggests(
                                        ClaimSuggestions.LEAVABLE_CLAIMS
                                )
                                .executes(context ->
                                        leaveNamedClaim(
                                                context.getSource(),
                                                StringArgumentType.getString(
                                                        context,
                                                        "claim"
                                                )
                                        )
                                )
                );
    }

    private static int leaveCurrentClaim(
            CommandSourceStack source
    ) throws CommandSyntaxException {
        ServerPlayer player =
                source.getPlayerOrException();

        Optional<Claim> optionalClaim =
                ClaimResolver.findAtPlayer(player);

        if (optionalClaim.isEmpty()) {
            source.sendFailure(
                    Component.literal(
                            "There is no claim here."
                    )
            );

            return 0;
        }

        return leaveClaim(
                source,
                player,
                optionalClaim.get()
        );
    }

    private static int leaveNamedClaim(
            CommandSourceStack source,
            String claimName
    ) throws CommandSyntaxException {
        ServerPlayer player =
                source.getPlayerOrException();

        Optional<Claim> optionalClaim =
                ClaimResolver.findParticipatingByName(
                        player.getUUID(),
                        claimName
                );

        if (optionalClaim.isEmpty()) {
            source.sendFailure(
                    Component.literal(
                            "You do not participate in a claim named \""
                                    + claimName
                                    + "\"."
                    )
            );

            return 0;
        }

        return leaveClaim(
                source,
                player,
                optionalClaim.get()
        );
    }

    private static int leaveClaim(
            CommandSourceStack source,
            ServerPlayer player,
            Claim claim
    ) {
        ClaimMutationResult result =
                Bananaclaims.CLAIM_MANAGER.leaveClaim(
                        claim,
                        player.getUUID()
                );

        return switch (result) {
            case MEMBER_LEFT -> {
                source.sendSuccess(
                        () -> Component.literal(
                                "You have left claim \""
                                        + claim.getName()
                                        + "\"."
                        ),
                        false
                );

                notifyOwner(
                        source,
                        claim,
                        player,
                        player.getName().getString()
                                + " left claim \""
                                + claim.getName()
                                + "\"."
                );

                yield 1;
            }

            case SUBOWNER_STEPPED_DOWN -> {
                source.sendSuccess(
                        () -> Component.literal(
                                "You stepped down as a subowner of claim \""
                                        + claim.getName()
                                        + "\" and remain a regular member."
                        ),
                        false
                );

                notifyOwner(
                        source,
                        claim,
                        player,
                        player.getName().getString()
                                + " stepped down as a subowner of claim \""
                                + claim.getName()
                                + "\" and remains a member."
                );

                yield 1;
            }

            case OWNER_CANNOT_LEAVE -> {
                source.sendFailure(
                        Component.literal(
                                "You own claim \""
                                        + claim.getName()
                                        + "\". Transfer ownership or delete the claim before leaving."
                        )
                );

                yield 0;
            }

            case NOT_PARTICIPANT -> {
                source.sendFailure(
                        Component.literal(
                                "You are not a member or subowner of claim \""
                                        + claim.getName()
                                        + "\"."
                        )
                );

                yield 0;
            }

            default -> {
                source.sendFailure(
                        Component.literal(
                                "Unable to leave claim \""
                                        + claim.getName()
                                        + "\"."
                        )
                );

                yield 0;
            }
        };
    }

    private static void notifyOwner(
            CommandSourceStack source,
            Claim claim,
            ServerPlayer leavingPlayer,
            String message
    ) {
        UUID ownerUuid = claim.getOwnerUuid();

        if (ownerUuid == null
                || ownerUuid.equals(leavingPlayer.getUUID())) {
            return;
        }

        source.getServer()
                .getPlayerList()
                .getPlayers()
                .stream()
                .filter(player ->
                        ownerUuid.equals(player.getUUID())
                )
                .findFirst()
                .ifPresent(owner ->
                        owner.sendSystemMessage(
                                Component.literal(message)
                        )
                );
    }
}
