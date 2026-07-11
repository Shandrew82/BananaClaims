package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.localization.BananaClaimsMessages;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.selection.ClaimSelection;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public class CreateAreaClaimCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("createarea")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(context -> createArea(
                                context.getSource(),
                                StringArgumentType.getString(context, "name")
                        ))
                );
    }

    private static int createArea(
            CommandSourceStack source,
            String claimName
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        ClaimSelection selection = Bananaclaims.SELECTION_MANAGER
                .getSelection(player.getUUID());

        if (selection == null || !selection.hasBothPositions()) {
            source.sendFailure(
                    BananaClaimsMessages.text("command.bananaclaims.createarea.positions_required")
            );
            return 0;
        }

        if (!selection.isSameDimension()) {
            source.sendFailure(
                    BananaClaimsMessages.text("command.bananaclaims.createarea.same_dimension")
            );
            return 0;
        }

        boolean duplicateName = Bananaclaims.CLAIM_MANAGER
                .getAllClaims()
                .stream()
                .anyMatch(claim -> claim.getName().equalsIgnoreCase(claimName));

        if (duplicateName) {
            source.sendFailure(
                    BananaClaimsMessages.text("command.bananaclaims.createarea.duplicate_name", claimName)
            );
            return 0;
        }

        BlockPos pos1 = selection.getPos1();
        BlockPos pos2 = selection.getPos2();
        String dimension = selection.getPos1Dimension();

        int pos1ChunkX = Math.floorDiv(pos1.getX(), 16);
        int pos1ChunkZ = Math.floorDiv(pos1.getZ(), 16);

        int pos2ChunkX = Math.floorDiv(pos2.getX(), 16);
        int pos2ChunkZ = Math.floorDiv(pos2.getZ(), 16);

        int minChunkX = Math.min(pos1ChunkX, pos2ChunkX);
        int maxChunkX = Math.max(pos1ChunkX, pos2ChunkX);

        int minChunkZ = Math.min(pos1ChunkZ, pos2ChunkZ);
        int maxChunkZ = Math.max(pos1ChunkZ, pos2ChunkZ);

        int chunkCount = (maxChunkX - minChunkX + 1)
                * (maxChunkZ - minChunkZ + 1);

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (Bananaclaims.CLAIM_MANAGER.isChunkClaimed(
                        dimension,
                        chunkX,
                        chunkZ
                )) {
                    source.sendFailure(
                            BananaClaimsMessages.text("command.bananaclaims.createarea.overlap")
                    );
                    return 0;
                }
            }
        }

        Claim claim = new Claim(
                claimName,
                player.getUUID(),
                player.getName().getString(),
                dimension,
                minChunkX,
                minChunkZ
        );

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                claim.addChunk(dimension, chunkX, chunkZ);
            }
        }

        boolean created = Bananaclaims.CLAIM_MANAGER.addClaim(claim);

        if (!created) {
            source.sendFailure(
                    BananaClaimsMessages.text("command.bananaclaims.createarea.failed")
            );
            return 0;
        }

        Bananaclaims.SELECTION_MANAGER.clearSelection(player.getUUID());
        Bananaclaims.DISPLAY_PREVIEW_V2_MANAGER.stop(player.getUUID());

        source.sendSuccess(
                () -> BananaClaimsMessages.text("command.bananaclaims.createarea.success", claimName, chunkCount),
                false
        );

        return 1;
    }
}
