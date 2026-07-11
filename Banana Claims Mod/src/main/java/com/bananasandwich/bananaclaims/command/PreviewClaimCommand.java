package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.localization.BananaClaimsMessages;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.Optional;

public final class PreviewClaimCommand {

    private PreviewClaimCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("preview")
                .executes(context ->
                        previewCurrentClaim(
                                context.getSource()
                        )
                )
                .then(Commands.literal("stop")
                        .executes(context ->
                                stopPreview(
                                        context.getSource()
                                )
                        )
                )
                .then(Commands.literal("nearest")
                        .executes(context ->
                                previewNearestManagedClaim(
                                        context.getSource()
                                )
                        )
                )
                .then(Commands.argument(
                                        "claim",
                                        StringArgumentType.word()
                                )
                                .suggests(
                                        ClaimSuggestions.MANAGED_CLAIMS
                                )
                                .executes(context ->
                                        previewNamedClaim(
                                                context.getSource(),
                                                StringArgumentType.getString(
                                                        context,
                                                        "claim"
                                                )
                                        )
                                )
                );
    }

    private static int previewCurrentClaim(
            CommandSourceStack source
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        Optional<Claim> optionalClaim =
                ClaimResolver.findAtPlayer(player);

        if (optionalClaim.isEmpty()) {
            source.sendFailure(
                    BananaClaimsMessages.text("command.bananaclaims.error.no_claim_here")
            );

            return 0;
        }

        return showClaimPreview(
                source,
                player,
                optionalClaim.get()
        );
    }

    private static int previewNamedClaim(
            CommandSourceStack source,
            String claimName
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        Optional<Claim> optionalClaim =
                ClaimResolver.findManagedByName(
                        player.getUUID(),
                        claimName
                );

        if (optionalClaim.isEmpty()) {
            source.sendFailure(
                    BananaClaimsMessages.text("command.bananaclaims.error.cannot_manage_named", claimName)
            );

            return 0;
        }

        return showClaimPreview(
                source,
                player,
                optionalClaim.get()
        );
    }

    private static int previewNearestManagedClaim(
            CommandSourceStack source
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String dimension = player.level().dimension().toString();

        Optional<Claim> optionalClaim =
                Bananaclaims.CLAIM_MANAGER
                        .getAllClaims()
                        .stream()
                        .filter(claim ->
                                claim.canManage(player.getUUID())
                                        && dimension.equals(
                                        claim.getDimension()
                                )
                        )
                        .min(
                                Comparator.comparingDouble(
                                        claim -> distanceSquaredToClaim(
                                                player,
                                                claim
                                        )
                                )
                        );

        if (optionalClaim.isEmpty()) {
            source.sendFailure(
                    BananaClaimsMessages.text("command.bananaclaims.preview.no_managed_dimension")
            );

            return 0;
        }

        return showClaimPreview(
                source,
                player,
                optionalClaim.get()
        );
    }

    private static int stopPreview(
            CommandSourceStack source
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        boolean stopped =
                Bananaclaims.DISPLAY_PREVIEW_V2_MANAGER.stop(
                        player.getUUID()
                );

        if (!stopped) {
            source.sendFailure(
                    BananaClaimsMessages.text("command.bananaclaims.preview.none_active")
            );

            return 0;
        }

        source.sendSuccess(
                () -> BananaClaimsMessages.text("command.bananaclaims.preview.stopped"),
                false
        );

        return 1;
    }

    private static int showClaimPreview(
            CommandSourceStack source,
            ServerPlayer player,
            Claim claim
    ) {
        ServerLevel level = player.level();

        if (!level.dimension()
                .toString()
                .equals(claim.getDimension())) {
            source.sendFailure(
                    BananaClaimsMessages.text("command.bananaclaims.preview.other_dimension")
            );

            return 0;
        }

        boolean shown =
                Bananaclaims.DISPLAY_PREVIEW_V2_MANAGER
                        .showClaimDisplay(
                                player,
                                claim
                        );

        if (!shown) {
            source.sendFailure(
                    BananaClaimsMessages.text("command.bananaclaims.preview.failed")
            );

            return 0;
        }

        String duration =
                Bananaclaims.PREVIEW_V2_CONFIG_MANAGER
                        .getDurationDescription();

        source.sendSuccess(
                () -> BananaClaimsMessages.text("command.bananaclaims.preview.success", claim.getName(), duration),
                false
        );

        return 1;
    }

    private static double distanceSquaredToClaim(
            ServerPlayer player,
            Claim claim
    ) {
        return claim.getChunks()
                .stream()
                .mapToDouble(chunk -> {
                    double centerX = chunk.getChunkX() * 16.0D + 8.0D;
                    double centerZ = chunk.getChunkZ() * 16.0D + 8.0D;
                    double deltaX = player.getX() - centerX;
                    double deltaZ = player.getZ() - centerZ;

                    return deltaX * deltaX + deltaZ * deltaZ;
                })
                .min()
                .orElse(Double.MAX_VALUE);
    }
}



