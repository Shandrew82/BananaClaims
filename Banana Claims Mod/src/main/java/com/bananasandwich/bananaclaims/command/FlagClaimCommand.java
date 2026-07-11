package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.localization.BananaClaimsMessages;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.claim.ClaimFlagDefinition;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

public final class FlagClaimCommand {

    private FlagClaimCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("flag")
                .then(Commands.argument(
                                        "claim",
                                        StringArgumentType.word()
                                )
                                .suggests(
                                        ClaimSuggestions.MANAGED_CLAIMS
                                )
                                .then(Commands.argument(
                                                        "flag",
                                                        StringArgumentType.word()
                                                )
                                                .suggests((context, builder) ->
                                                        SharedSuggestionProvider.suggest(
                                                                ClaimFlagDefinition.canonicalNames(),
                                                                builder
                                                        )
                                                )
                                                .executes(context ->
                                                        showFlag(
                                                                context.getSource(),
                                                                StringArgumentType.getString(
                                                                        context,
                                                                        "claim"
                                                                ),
                                                                StringArgumentType.getString(
                                                                        context,
                                                                        "flag"
                                                                )
                                                        )
                                                )
                                                .then(Commands.argument(
                                                                        "value",
                                                                        BoolArgumentType.bool()
                                                                )
                                                                .suggests((context, builder) ->
                                                                        SharedSuggestionProvider.suggest(
                                                                                List.of(
                                                                                        "true",
                                                                                        "false"
                                                                                ),
                                                                                builder
                                                                        )
                                                                )
                                                                .executes(context ->
                                                                        updateFlag(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(
                                                                                        context,
                                                                                        "claim"
                                                                                ),
                                                                                StringArgumentType.getString(
                                                                                        context,
                                                                                        "flag"
                                                                                ),
                                                                                BoolArgumentType.getBool(
                                                                                        context,
                                                                                        "value"
                                                                                )
                                                                        )
                                                                )
                                                )
                                )
                );
    }

    private static int showFlag(
            CommandSourceStack source,
            String claimName,
            String flagName
    ) {
        Optional<Claim> optionalClaim = resolveManagedClaim(
                source,
                claimName
        );

        if (optionalClaim.isEmpty()) {
            return 0;
        }

        Optional<ClaimFlagDefinition> optionalFlag =
                ClaimFlagDefinition.findCommandFlag(flagName);

        if (optionalFlag.isEmpty()) {
            sendUnknownFlag(source, flagName);
            return 0;
        }

        Claim claim = optionalClaim.get();
        ClaimFlagDefinition flag = optionalFlag.get();
        boolean value = flag.getValue(claim.getFlags());

        source.sendSuccess(
                () -> BananaClaimsMessages.text("command.bananaclaims.flag.status", flag.getCanonicalName(), value, claim.getName()),
                false
        );

        return 1;
    }

    private static int updateFlag(
            CommandSourceStack source,
            String claimName,
            String flagName,
            boolean value
    ) {
        Optional<Claim> optionalClaim = resolveManagedClaim(
                source,
                claimName
        );

        if (optionalClaim.isEmpty()) {
            return 0;
        }

        Claim claim = optionalClaim.get();
        ServerPlayer player = source.getPlayer();

        if (player == null
                || !claim.canEditFlags(player.getUUID())) {
            source.sendFailure(
                    BananaClaimsMessages.text("command.bananaclaims.flag.cannot_edit", claim.getName())
            );

            return 0;
        }

        Optional<ClaimFlagDefinition> optionalFlag =
                ClaimFlagDefinition.findCommandFlag(flagName);

        if (optionalFlag.isEmpty()) {
            sendUnknownFlag(source, flagName);
            return 0;
        }

        ClaimFlagDefinition flag = optionalFlag.get();
        boolean previousValue = flag.getValue(claim.getFlags());

        if (previousValue == value) {
            source.sendSuccess(
                    () -> BananaClaimsMessages.text("command.bananaclaims.flag.already", flag.getCanonicalName(), value, claim.getName()),
                    false
            );

            return 1;
        }

        flag.setValue(
                claim.getFlags(),
                value
        );

        Bananaclaims.CLAIM_MANAGER.saveClaims();

        source.sendSuccess(
                () -> BananaClaimsMessages.text("command.bananaclaims.flag.success", flag.getCanonicalName(), value, claim.getName()),
                false
        );

        return 1;
    }

    private static Optional<Claim> resolveManagedClaim(
            CommandSourceStack source,
            String claimName
    ) {
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(
                    BananaClaimsMessages.text("command.bananaclaims.flag.players_only")
            );

            return Optional.empty();
        }

        Optional<Claim> optionalClaim =
                ClaimResolver.findManagedByName(
                        player.getUUID(),
                        claimName
                );

        if (optionalClaim.isEmpty()) {
            source.sendFailure(
                    BananaClaimsMessages.text("command.bananaclaims.error.cannot_manage_named", claimName)
            );
        }

        return optionalClaim;
    }

    private static void sendUnknownFlag(
            CommandSourceStack source,
            String flagName
    ) {
        source.sendFailure(
                BananaClaimsMessages.text("command.bananaclaims.flag.unknown", flagName, String.join(", ", ClaimFlagDefinition.canonicalNames()))
        );
    }
}
