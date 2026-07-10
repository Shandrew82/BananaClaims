package com.bananasandwich.bananaclaims.command.member;

import com.bananasandwich.bananaclaims.command.ClaimSuggestions;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class MemberCommand {

    private MemberCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("member")

                .then(Commands.literal("add")
                        .then(AddMemberCommand.localPlayerArgument())
                )

                .then(Commands.literal("remove")
                        .then(RemoveMemberCommand.localPlayerArgument())
                )

                .then(Commands.literal("list")
                        .executes(context ->
                                ListMemberCommand.listCurrentClaim(
                                        context.getSource()
                                )
                        )
                )

                .then(Commands.argument(
                                        "claim",
                                        StringArgumentType.word()
                                )
                                .suggests(
                                        ClaimSuggestions.OWNED_CLAIMS
                                )

                                .then(Commands.literal("add")
                                        .then(AddMemberCommand.remotePlayerArgument())
                                )

                                .then(Commands.literal("remove")
                                        .then(RemoveMemberCommand.remotePlayerArgument())
                                )

                                .then(Commands.literal("list")
                                        .executes(context ->
                                                ListMemberCommand.listNamedClaim(
                                                        context.getSource(),
                                                        StringArgumentType.getString(
                                                                context,
                                                                "claim"
                                                        )
                                                )
                                        )
                                )
                );
    }
}