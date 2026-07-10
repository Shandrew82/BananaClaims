package com.bananasandwich.bananaclaims.command.member;

import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.claim.ClaimMember;
import com.bananasandwich.bananaclaims.command.ClaimResolver;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

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
                    Component.literal(
                            "There is no claim here."
                    )
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
                    Component.literal(
                            "No claim found named \""
                                    + claimName
                                    + "\"."
                    )
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

        if (memberNames.isEmpty()) {
            source.sendSuccess(
                    () -> Component.literal(
                            "Claim \""
                                    + claim.getName()
                                    + "\" has no members."
                    ),
                    false
            );

            return 1;
        }

        String memberList = String.join(
                "\n- ",
                memberNames
        );

        source.sendSuccess(
                () -> Component.literal(
                        "Members of claim \""
                                + claim.getName()
                                + "\" ("
                                + memberNames.size()
                                + "):\n- "
                                + memberList
                ),
                false
        );

        return 1;
    }
}