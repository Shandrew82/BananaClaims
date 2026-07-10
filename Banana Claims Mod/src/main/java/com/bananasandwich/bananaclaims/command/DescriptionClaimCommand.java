package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class DescriptionClaimCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("description")
                .then(Commands.argument(
                                        "input",
                                        StringArgumentType.greedyString()
                                )
                                .suggests(ClaimSuggestions.MANAGED_CLAIMS)
                                .executes(context -> updateDescription(
                                        context.getSource(),
                                        StringArgumentType.getString(
                                                context,
                                                "input"
                                        )
                                ))
                );
    }

    private static int updateDescription(
            CommandSourceStack source,
            String input
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        String trimmedInput = input == null
                ? ""
                : input.trim();

        if (trimmedInput.isBlank()) {
            source.sendFailure(
                    Component.literal(
                            "You must provide a description."
                    )
            );

            return 0;
        }

        int firstSpace = trimmedInput.indexOf(' ');

        if (firstSpace > 0) {
            String possibleClaimName =
                    trimmedInput.substring(0, firstSpace);

            String possibleDescription =
                    trimmedInput.substring(firstSpace + 1).trim();

            Optional<Claim> namedClaim =
                    ClaimResolver.findManagedByName(
                            player.getUUID(),
                            possibleClaimName
                    );

            if (namedClaim.isPresent()
                    && !possibleDescription.isBlank()) {
                return setDescription(
                        source,
                        namedClaim.get(),
                        possibleDescription
                );
            }
        }

        Optional<Claim> currentClaim =
                ClaimResolver.findAtPlayer(player);

        if (currentClaim.isEmpty()) {
            source.sendFailure(
                    Component.literal(
                            "There is no claim here. "
                                    + "To update a claim remotely, use "
                                    + "/claim description <claim> <description>."
                    )
            );

            return 0;
        }

        Claim claim = currentClaim.get();

        if (!claim.canManage(player.getUUID())) {
            source.sendFailure(
                    Component.literal(
                            "You cannot manage this claim."
                    )
            );

            return 0;
        }

        return setDescription(
                source,
                claim,
                trimmedInput
        );
    }

    private static int setDescription(
            CommandSourceStack source,
            Claim claim,
            String description
    ) {
        claim.setDescription(description);
        Bananaclaims.CLAIM_MANAGER.saveClaims();

        source.sendSuccess(
                () -> Component.literal(
                        "Updated the description for claim \""
                                + claim.getName()
                                + "\"."
                ),
                false
        );

        return 1;
    }
}