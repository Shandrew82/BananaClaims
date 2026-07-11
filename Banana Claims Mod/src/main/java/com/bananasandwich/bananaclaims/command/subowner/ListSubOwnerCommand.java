package com.bananasandwich.bananaclaims.command.subowner;

import com.bananasandwich.bananaclaims.localization.BananaClaimsMessages;

import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.claim.ClaimSubOwner;
import com.bananasandwich.bananaclaims.command.ClaimResolver;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

public final class ListSubOwnerCommand {

    private ListSubOwnerCommand() {
    }

    public static int listCurrentClaim(
            CommandSourceStack source
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<Claim> optionalClaim = ClaimResolver.findAtPlayer(player);

        if (optionalClaim.isEmpty()) {
            source.sendFailure(BananaClaimsMessages.text("command.bananaclaims.error.no_claim_here"));
            return 0;
        }

        return sendSubOwnerList(source, optionalClaim.get());
    }

    public static int listNamedClaim(
            CommandSourceStack source,
            String claimName
    ) {
        Optional<Claim> optionalClaim = ClaimResolver.findByName(claimName);

        if (optionalClaim.isEmpty()) {
            source.sendFailure(BananaClaimsMessages.text("command.bananaclaims.error.no_claim_named", claimName));
            return 0;
        }

        return sendSubOwnerList(source, optionalClaim.get());
    }

    private static int sendSubOwnerList(
            CommandSourceStack source,
            Claim claim
    ) {
        List<String> names = claim.getSubOwners()
                .stream()
                .map(ClaimSubOwner::getName)
                .filter(name -> name != null && !name.isBlank())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        String list = names.isEmpty()
                ? "None"
                : "\n- " + String.join("\n- ", names);

        source.sendSuccess(
                () -> BananaClaimsMessages.text("command.bananaclaims.subowner.list", claim.getName(), names.size(), list),
                false
        );

        return 1;
    }
}
