package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.claim.ClaimMember;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

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
                    () -> Component.literal(
                            "You are standing in Wilderness."
                    ),
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
                    Component.literal(
                            "No claim found named \""
                                    + claimName
                                    + "\"."
                    )
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
                        ? "No description."
                        : claim.getDescription();

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
                        ? "None"
                        : String.join(", ", memberNames);

        source.sendSuccess(
                () -> Component.literal(
                        "Claim: "
                                + claim.getName()
                                + "\nOwner: "
                                + claim.getOwnerName()
                                + "\nMembers ("
                                + memberNames.size()
                                + "): "
                                + membersText
                                + "\nDescription: "
                                + description
                                + "\nChunks: "
                                + claim.getChunks().size()
                ),
                false
        );
    }
}