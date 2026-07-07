package com.bananasandwich.bananaclaims.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ClaimCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("claim")
                        .executes(context -> {
                            context.getSource().sendSuccess(
                                    () -> Component.literal("Banana Claims is loaded."),
                                    false
                            );
                            return 1;
                        })

                        .then(CreateClaimCommand.register())
                        .then(InfoClaimCommand.register())
        );
    }
}