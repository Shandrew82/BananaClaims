package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.localization.BananaClaimsMessages;
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
                    BananaClaimsMessages.text("command.bananaclaims.error.no_claim_here")
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
                    BananaClaimsMessages.text("command.bananaclaims.error.not_owner_named", claimName)
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
                    BananaClaimsMessages.text("command.bananaclaims.error.online_player_not_found", playerName)
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
                        () -> BananaClaimsMessages.text("command.bananaclaims.transfer.success_actor", claim.getName(), target.getName().getString()),
                        false
                );

                target.sendSystemMessage(
                        BananaClaimsMessages.text("command.bananaclaims.transfer.success_target", previousOwnerName, claim.getName())
                );

                yield 1;
            }

            case SAME_OWNER -> {
                source.sendFailure(
                        BananaClaimsMessages.text("command.bananaclaims.transfer.same_owner", target.getName().getString())
                );

                yield 0;
            }

            case DUPLICATE_OWNER_CLAIM_NAME -> {
                source.sendFailure(
                        BananaClaimsMessages.text("command.bananaclaims.transfer.duplicate_name", target.getName().getString(), claim.getName())
                );

                yield 0;
            }

            case NOT_AUTHORIZED -> {
                source.sendFailure(
                        BananaClaimsMessages.text("command.bananaclaims.transfer.not_owner")
                );

                yield 0;
            }

            default -> {
                source.sendFailure(
                        BananaClaimsMessages.text("command.bananaclaims.transfer.failed")
                );

                yield 0;
            }
        };
    }
}
