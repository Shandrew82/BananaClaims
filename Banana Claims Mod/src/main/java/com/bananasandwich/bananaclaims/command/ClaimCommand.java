package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.localization.BananaClaimsMessages;
import com.bananasandwich.bananaclaims.command.admin.AdminClaimCommand;
import com.bananasandwich.bananaclaims.command.member.MemberCommand;
import com.bananasandwich.bananaclaims.command.subowner.SubOwnerCommand;
import com.bananasandwich.bananaclaims.permission.ClaimPermission;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class ClaimCommand {

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("claim")
                        .executes(context -> {
                            if (!Bananaclaims.PERMISSION_SERVICE.has(
                                    context.getSource(),
                                    ClaimPermission.COMMAND_ROOT
                            )) {
                                context.getSource().sendFailure(
                                        BananaClaimsMessages.text("command.bananaclaims.permission_denied")
                                );

                                return 0;
                            }

                            context.getSource().sendSuccess(
                                    () -> BananaClaimsMessages.text("command.bananaclaims.loaded"),
                                    false
                            );

                            return 1;
                        })
                        .then(require(
                                SelectionClaimCommand.registerPos1(),
                                ClaimPermission.POS1
                        ))
                        .then(require(
                                SelectionClaimCommand.registerPos2(),
                                ClaimPermission.POS2
                        ))
                        .then(require(
                                CreateAreaClaimCommand.register(),
                                ClaimPermission.CREATE_AREA
                        ))
                        .then(require(
                                CreateClaimCommand.register(),
                                ClaimPermission.CREATE
                        ))
                        .then(require(
                                ExpandClaimCommand.register(),
                                ClaimPermission.EXPAND
                        ))
                        .then(require(
                                ShrinkClaimCommand.register(),
                                ClaimPermission.SHRINK
                        ))
                        .then(require(
                                PreviewClaimCommand.register(),
                                ClaimPermission.PREVIEW
                        ))
                        .then(require(
                                LeaveClaimCommand.register(),
                                ClaimPermission.LEAVE
                        ))
                        .then(require(
                                InfoClaimCommand.register(),
                                ClaimPermission.INFO
                        ))
                        .then(require(
                                DeleteClaimCommand.register(),
                                ClaimPermission.DELETE
                        ))
                        .then(require(
                                ListClaimCommand.register(),
                                ClaimPermission.LIST
                        ))
                        .then(require(
                                RenameClaimCommand.register(),
                                ClaimPermission.RENAME
                        ))
                        .then(require(
                                DescriptionClaimCommand.register(),
                                ClaimPermission.DESCRIPTION
                        ))
                        .then(require(
                                MemberCommand.register(),
                                ClaimPermission.MEMBER
                        ))
                        .then(require(
                                SubOwnerCommand.register(),
                                ClaimPermission.SUBOWNER
                        ))
                        .then(require(
                                TransferClaimCommand.register(),
                                ClaimPermission.TRANSFER
                        ))
                        .then(require(
                                FlagClaimCommand.register(),
                                ClaimPermission.FLAG
                        ))
                        .then(require(
                                PopupClaimCommand.register(),
                                ClaimPermission.POPUP
                        ))
                        .then(AdminClaimCommand.register())
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> require(
            LiteralArgumentBuilder<CommandSourceStack> command,
            ClaimPermission permission
    ) {
        return command.requires(
                Bananaclaims.PERMISSION_SERVICE.require(permission)
        );
    }
}
