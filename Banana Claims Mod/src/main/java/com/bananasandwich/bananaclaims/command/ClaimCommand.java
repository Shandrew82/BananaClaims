package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.command.member.MemberCommand;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ClaimCommand {

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("claim")
                        .executes(context -> {
                            context.getSource().sendSuccess(
                                    () -> Component.literal(
                                            "Banana Claims is loaded."
                                    ),
                                    false
                            );

                            return 1;
                        })

                        .then(SelectionClaimCommand.registerPos1())
                        .then(SelectionClaimCommand.registerPos2())
                        .then(CreateAreaClaimCommand.register())
                        .then(CreateClaimCommand.register())
                        .then(ExpandClaimCommand.register())
                        .then(ShrinkClaimCommand.register())
                        .then(InfoClaimCommand.register())
                        .then(DeleteClaimCommand.register())
                        .then(ListClaimCommand.register())
                        .then(RenameClaimCommand.register())
                        .then(DescriptionClaimCommand.register())
                        .then(MemberCommand.register())
                        .then(TransferClaimCommand.register())
                        .then(FlagClaimCommand.register())
                        .then(PopupClaimCommand.register())
        );
    }
}
