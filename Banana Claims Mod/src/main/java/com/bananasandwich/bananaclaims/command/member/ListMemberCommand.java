package com.bananasandwich.bananaclaims.command.member;

import com.bananasandwich.bananaclaims.localization.BananaClaimsMessages;

import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.claim.ClaimMember;
import com.bananasandwich.bananaclaims.claim.ClaimSubOwner;
import com.bananasandwich.bananaclaims.command.ClaimResolver;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ListMemberCommand {

    private ListMemberCommand() {
    }

    public static int listCurrentClaim(
            CommandSourceStack source
    ) throws CommandSyntaxException {
        ServerPlayer player =
                source.getPlayerOrException();

        Optional<Claim> optionalClaim =
                ClaimResolver.findAtPlayer(player);

        if (optionalClaim.isEmpty()) {
            source.sendFailure(
                    BananaClaimsMessages.text("command.bananaclaims.error.no_claim_here")
            );

            return 0;
        }

        return sendMemberList(
                source,
                optionalClaim.get()
        );
    }

    public static int listNamedClaim(
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

        return sendMemberList(
                source,
                optionalClaim.get()
        );
    }

    private static int sendMemberList(
            CommandSourceStack source,
            Claim claim
    ) {
        List<String> people = new ArrayList<>();

        String ownerName = claim.getOwnerName();

        if (!ownerName.isBlank()) {
            people.add(ownerName + " (Owner)");
        }

        claim.getSubOwners()
                .stream()
                .map(ClaimSubOwner::getName)
                .filter(name ->
                        name != null
                                && !name.isBlank()
                )
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .map(name -> name + " (Subowner)")
                .forEach(people::add);

        claim.getMembers()
                .stream()
                .map(ClaimMember::getName)
                .filter(name ->
                        name != null
                                && !name.isBlank()
                )
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(people::add);

        String peopleList = String.join(
                "\n- ",
                people
        );

        source.sendSuccess(
                () -> BananaClaimsMessages.text("command.bananaclaims.member.list", claim.getName(), people.size(), peopleList),
                false
        );

        return 1;
    }
}