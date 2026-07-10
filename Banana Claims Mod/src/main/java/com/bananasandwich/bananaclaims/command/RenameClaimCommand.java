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

public class RenameClaimCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("rename")
                .then(Commands.argument(
                                        "firstName",
                                        StringArgumentType.word()
                                )
                                .suggests(
                                        ClaimSuggestions.MANAGED_CLAIMS
                                )
                                .executes(context ->
                                        renameCurrentClaim(
                                                context.getSource(),
                                                StringArgumentType.getString(
                                                        context,
                                                        "firstName"
                                                )
                                        )
                                )
                                .then(Commands.argument(
                                                        "newName",
                                                        StringArgumentType.word()
                                                )
                                                .executes(context ->
                                                        renameNamedClaim(
                                                                context.getSource(),
                                                                StringArgumentType.getString(
                                                                        context,
                                                                        "firstName"
                                                                ),
                                                                StringArgumentType.getString(
                                                                        context,
                                                                        "newName"
                                                                )
                                                        )
                                                )
                                )
                );
    }

    private static int renameCurrentClaim(
            CommandSourceStack source,
            String newName
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

        Claim claim = optionalClaim.get();

        if (!claim.canManage(player.getUUID())) {
            source.sendFailure(
                    Component.literal(
                            "You cannot manage this claim."
                    )
            );

            return 0;
        }

        return renameClaim(
                source,
                claim,
                newName
        );
    }

    private static int renameNamedClaim(
            CommandSourceStack source,
            String claimName,
            String newName
    ) throws CommandSyntaxException {
        ServerPlayer player =
                source.getPlayerOrException();

        Optional<Claim> optionalClaim =
                ClaimResolver.findManagedByName(
                        player.getUUID(),
                        claimName
                );

        if (optionalClaim.isEmpty()) {
            source.sendFailure(
                    Component.literal(
                            "You cannot manage a claim named \""
                                    + claimName
                                    + "\"."
                    )
            );

            return 0;
        }

        return renameClaim(
                source,
                optionalClaim.get(),
                newName
        );
    }

    private static int renameClaim(
            CommandSourceStack source,
            Claim claim,
            String newName
    ) {
        Optional<Claim> duplicateClaim =
                ClaimResolver.findByName(newName);

        if (duplicateClaim.isPresent()
                && !duplicateClaim.get()
                .getClaimId()
                .equals(claim.getClaimId())) {
            source.sendFailure(
                    Component.literal(
                            "A claim named \""
                                    + newName
                                    + "\" already exists."
                    )
            );

            return 0;
        }

        String oldName = claim.getName();

        claim.setName(newName);
        Bananaclaims.CLAIM_MANAGER.saveClaims();

        source.sendSuccess(
                () -> Component.literal(
                        "Claim renamed from \""
                                + oldName
                                + "\" to \""
                                + newName
                                + "\"."
                ),
                false
        );

        return 1;
    }
}