package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.claim.ClaimMutationResult;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

public final class TransferClaimCommand {

    private TransferClaimCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("transfer")
                .then(
                        Commands.argument(
                                        "claimOrPlayer",
                                        StringArgumentType.word()
                                )
                                .suggests(
                                        TransferClaimCommand::suggestClaimsAndPlayers
                                )
                                .executes(context ->
                                        transferCurrentClaim(
                                                context.getSource(),
                                                StringArgumentType.getString(
                                                        context,
                                                        "claimOrPlayer"
                                                )
                                        )
                                )
                                .then(
                                        Commands.argument(
                                                        "player",
                                                        StringArgumentType.word()
                                                )
                                                .suggests(
                                                        ClaimSuggestions.ONLINE_PLAYERS
                                                )
                                                .executes(context ->
                                                        transferNamedClaim(
                                                                context.getSource(),
                                                                StringArgumentType.getString(
                                                                        context,
                                                                        "claimOrPlayer"
                                                                ),
                                                                StringArgumentType.getString(
                                                                        context,
                                                                        "player"
                                                                )
                                                        )
                                                )
                                )
                );
    }

    private static CompletableFuture<Suggestions> suggestClaimsAndPlayers(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) throws CommandSyntaxException {
        ServerPlayer owner =
                context.getSource().getPlayerOrException();

        Set<String> suggestions = new TreeSet<>(
                String.CASE_INSENSITIVE_ORDER
        );

        Bananaclaims.CLAIM_MANAGER
                .getClaimsForOwner(owner.getUUID())
                .stream()
                .map(Claim::getName)
                .filter(name ->
                        name != null && !name.isBlank()
                )
                .forEach(suggestions::add);

        context.getSource()
                .getServer()
                .getPlayerList()
                .getPlayers()
                .stream()
                .map(player ->
                        player.getName().getString()
                )
                .filter(name ->
                        !name.equalsIgnoreCase(
                                owner.getName().getString()
                        )
                )
                .forEach(suggestions::add);

        suggestions.stream()
                .sorted(
                        Comparator.comparing(
                                String::toLowerCase
                        )
                )
                .forEach(builder::suggest);

        return builder.buildFuture();
    }

    private static int transferCurrentClaim(
            CommandSourceStack source,
            String playerName
    ) throws CommandSyntaxException {
        ServerPlayer currentOwner =
                source.getPlayerOrException();

        Optional<Claim> optionalClaim =
                ClaimResolver.findAtPlayer(currentOwner);

        if (optionalClaim.isEmpty()) {
            source.sendFailure(
                    Component.literal(
                            "There is no claim here."
                    )
            );

            return 0;
        }

        return transferOwnership(
                source,
                currentOwner,
                optionalClaim.get(),
                playerName
        );
    }

    private static int transferNamedClaim(
            CommandSourceStack source,
            String claimName,
            String playerName
    ) throws CommandSyntaxException {
        ServerPlayer currentOwner =
                source.getPlayerOrException();

        Optional<Claim> optionalClaim =
                ClaimResolver.findOwnedByName(
                        currentOwner.getUUID(),
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

        return transferOwnership(
                source,
                currentOwner,
                optionalClaim.get(),
                playerName
        );
    }

    private static int transferOwnership(
            CommandSourceStack source,
            ServerPlayer currentOwner,
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
        String previousOwnerName = currentOwner.getName().getString();

        ClaimMutationResult result =
                Bananaclaims.CLAIM_MANAGER.transferOwnership(
                        claim,
                        currentOwner.getUUID(),
                        target.getUUID(),
                        target.getName().getString()
                );

        return switch (result) {
            case OWNERSHIP_TRANSFERRED -> {
                source.sendSuccess(
                        () -> Component.literal(
                                "Transferred ownership of claim \""
                                        + claim.getName()
                                        + "\" to "
                                        + target.getName().getString()
                                        + ". You are now a member of the claim."
                        ),
                        false
                );

                target.sendSystemMessage(
                        Component.literal(
                                previousOwnerName
                                        + " transferred ownership of claim \""
                                        + claim.getName()
                                        + "\" to you."
                        )
                );

                yield 1;
            }

            case SAME_OWNER -> {
                source.sendFailure(
                        Component.literal(
                                target.getName().getString()
                                        + " already owns this claim."
                        )
                );

                yield 0;
            }

            case DUPLICATE_OWNER_CLAIM_NAME -> {
                source.sendFailure(
                        Component.literal(
                                target.getName().getString()
                                        + " already owns a claim named \""
                                        + claim.getName()
                                        + "\". Rename one of the claims before transferring ownership."
                        )
                );

                yield 0;
            }

            case NOT_AUTHORIZED -> {
                source.sendFailure(
                        Component.literal(
                                "Only the current owner can transfer this claim."
                        )
                );

                yield 0;
            }

            default -> {
                source.sendFailure(
                        Component.literal(
                                "Unable to transfer ownership of that claim."
                        )
                );

                yield 0;
            }
        };
    }
}
