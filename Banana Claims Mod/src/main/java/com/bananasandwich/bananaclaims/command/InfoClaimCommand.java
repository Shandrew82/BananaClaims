package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.localization.BananaClaimsMessages;

import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.claim.ClaimMember;
import com.bananasandwich.bananaclaims.claim.ClaimSubOwner;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.util.List;
import java.util.Optional;

public class InfoClaimCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("info")
                .executes(context ->
                        showCurrentClaim(
                                context.getSource()
                        )
                )
                .then(Commands.argument(
                                        "claim",
                                        StringArgumentType.word()
                                )
                                .suggests(
                                        ClaimSuggestions.ALL_CLAIMS
                                )
                                .executes(context ->
                                        showNamedClaim(
                                                context.getSource(),
                                                StringArgumentType.getString(
                                                        context,
                                                        "claim"
                                                )
                                        )
                                )
                );
    }

    private static int showCurrentClaim(
            CommandSourceStack source
    ) {
        Optional<Claim> optionalClaim =
                ClaimResolver.findAtSource(source);

        if (optionalClaim.isEmpty()) {
            source.sendSuccess(
                    () -> BananaClaimsMessages.text("command.bananaclaims.info.wilderness"),
                    false
            );

            return 1;
        }

        sendClaimInformation(
                source,
                optionalClaim.get()
        );

        return 1;
    }

    private static int showNamedClaim(
            CommandSourceStack source,
            String claimName
    ) {
        Optional<Claim> optionalClaim =
                ClaimResolver.findByName(claimName);

        if (optionalClaim.isEmpty()) {
            source.sendFailure(
                    BananaClaimsMessages.text("command.bananaclaims.error.no_claim_named", claimName)
            );

            return 0;
        }

        sendClaimInformation(
                source,
                optionalClaim.get()
        );

        return 1;
    }

    private static void sendClaimInformation(
            CommandSourceStack source,
            Claim claim
    ) {
        String description =
                claim.getDescription().isBlank()
                        ? BananaClaimsMessages.string(
                        "command.bananaclaims.info.no_description"
                )
                        : claim.getDescription();

        List<String> subOwnerNames =
                claim.getSubOwners()
                        .stream()
                        .map(ClaimSubOwner::getName)
                        .filter(name ->
                                name != null
                                        && !name.isBlank()
                        )
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList();

        String subOwnersText =
                subOwnerNames.isEmpty()
                        ? BananaClaimsMessages.string(
                        "command.bananaclaims.info.none"
                )
                        : String.join(", ", subOwnerNames);

        List<String> memberNames =
                claim.getMembers()
                        .stream()
                        .map(ClaimMember::getName)
                        .filter(name ->
                                name != null
                                        && !name.isBlank()
                        )
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList();

        String membersText =
                memberNames.isEmpty()
                        ? BananaClaimsMessages.string(
                        "command.bananaclaims.info.none"
                )
                        : String.join(", ", memberNames);

        source.sendSuccess(
                () -> BananaClaimsMessages.text("command.bananaclaims.info.summary", claim.getName(), claim.getOwnerName(), subOwnerNames.size(), subOwnersText, memberNames.size(), membersText, description, claim.getChunks().size()),
                false
        );
    }
}