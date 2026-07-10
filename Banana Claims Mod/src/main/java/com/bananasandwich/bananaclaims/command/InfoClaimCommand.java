package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.claim.Claim;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

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

        source.sendSuccess(
                () -> Component.literal(
                        "Claim: " + claim.getName()
                                + "\nOwner: "
                                + claim.getOwnerName()
                                + "\nDescription: "
                                + description
                                + "\nChunks: "
                                + claim.getChunks().size()
                ),
                false
        );
    }
}