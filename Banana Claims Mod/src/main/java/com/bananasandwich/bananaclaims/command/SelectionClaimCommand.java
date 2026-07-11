package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.localization.BananaClaimsMessages;
import com.bananasandwich.bananaclaims.selection.ClaimSelection;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public class SelectionClaimCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> registerPos1() {
        return Commands.literal("pos1")
                .executes(context -> setPos1(context.getSource()));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> registerPos2() {
        return Commands.literal("pos2")
                .executes(context -> setPos2(context.getSource()));
    }

    private static int setPos1(
            CommandSourceStack source
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BlockPos position = player.blockPosition();
        String dimension = player.level().dimension().toString();

        Bananaclaims.SELECTION_MANAGER.setPos1(
                player.getUUID(),
                dimension,
                position
        );

        source.sendSuccess(
                () -> BananaClaimsMessages.text("command.bananaclaims.selection.pos1", position.getX(), position.getY(), position.getZ()),
                false
        );

        showSelectionPreviewIfReady(
                source,
                player
        );

        return 1;
    }

    private static int setPos2(
            CommandSourceStack source
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BlockPos position = player.blockPosition();
        String dimension = player.level().dimension().toString();

        Bananaclaims.SELECTION_MANAGER.setPos2(
                player.getUUID(),
                dimension,
                position
        );

        source.sendSuccess(
                () -> BananaClaimsMessages.text("command.bananaclaims.selection.pos2", position.getX(), position.getY(), position.getZ()),
                false
        );

        showSelectionPreviewIfReady(
                source,
                player
        );

        return 1;
    }

    private static void showSelectionPreviewIfReady(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        ClaimSelection selection =
                Bananaclaims.SELECTION_MANAGER.getSelection(
                        player.getUUID()
                );

        if (selection == null
                || !selection.hasBothPositions()) {
            return;
        }

        if (!selection.isSameDimension()) {
            stopPreview(player);

            source.sendFailure(
                    BananaClaimsMessages.text("command.bananaclaims.selection.same_dimension")
            );

            return;
        }

        boolean shown =
                Bananaclaims.DISPLAY_PREVIEW_V2_MANAGER
                        .showSelectionDisplay(
                                player,
                                selection
                        );

        if (!shown) {
            source.sendFailure(
                    BananaClaimsMessages.text("command.bananaclaims.selection.preview_failed")
            );

            return;
        }

        String duration =
                Bananaclaims.PREVIEW_V2_CONFIG_MANAGER
                        .getDurationDescription();

        source.sendSuccess(
                () -> BananaClaimsMessages.text("command.bananaclaims.selection.preview_success", duration),
                false
        );
    }

    private static void stopPreview(
            ServerPlayer player
    ) {
        Bananaclaims.DISPLAY_PREVIEW_V2_MANAGER.stop(
                player.getUUID()
        );
    }
}



