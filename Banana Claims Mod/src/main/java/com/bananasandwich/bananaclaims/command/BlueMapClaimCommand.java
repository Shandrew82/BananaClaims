package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.claim.ClaimBlueMapStyle;
import com.bananasandwich.bananaclaims.localization.BananaClaimsMessages;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class BlueMapClaimCommand {

    private BlueMapClaimCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("bluemap")
                .executes(context -> showCurrent(context.getSource()))
                .then(Commands.argument("claim", StringArgumentType.word())
                        .suggests(ClaimSuggestions.MANAGED_CLAIMS)
                        .executes(context -> showNamed(
                                context.getSource(),
                                StringArgumentType.getString(context, "claim")
                        ))
                        .then(Commands.literal("fill")
                                .then(Commands.argument("hex", StringArgumentType.greedyString())
                                        .executes(context -> setColor(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "claim"),
                                                StringArgumentType.getString(context, "hex"),
                                                true
                                        ))))
                        .then(Commands.literal("line")
                                .then(Commands.argument("hex", StringArgumentType.greedyString())
                                        .executes(context -> setColor(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "claim"),
                                                StringArgumentType.getString(context, "hex"),
                                                false
                                        ))))
                        .then(Commands.literal("fillopacity")
                                .then(Commands.argument("opacity", FloatArgumentType.floatArg(0.0F, 1.0F))
                                        .executes(context -> setOpacity(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "claim"),
                                                FloatArgumentType.getFloat(context, "opacity"),
                                                true
                                        ))))
                        .then(Commands.literal("lineopacity")
                                .then(Commands.argument("opacity", FloatArgumentType.floatArg(0.0F, 1.0F))
                                        .executes(context -> setOpacity(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "claim"),
                                                FloatArgumentType.getFloat(context, "opacity"),
                                                false
                                        ))))
                        .then(Commands.literal("linewidth")
                                .then(Commands.argument("width", IntegerArgumentType.integer(1, 10))
                                        .executes(context -> setLineWidth(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "claim"),
                                                IntegerArgumentType.getInteger(context, "width")
                                        ))))
                        .then(Commands.literal("reset")
                                .executes(context -> reset(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "claim")
                                ))));
    }

    private static int showCurrent(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<Claim> claim = ClaimResolver.findAtPlayer(player);
        if (claim.isEmpty()) {
            return failure(source, "command.bananaclaims.error.no_claim_here");
        }
        if (!claim.get().hasAccess(player.getUUID())) {
            return failure(source, "command.bananaclaims.error.cannot_manage");
        }
        return show(source, claim.get());
    }

    private static int showNamed(CommandSourceStack source, String claimName)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<Claim> claim = ClaimResolver.findManagedByName(player.getUUID(), claimName);
        if (claim.isEmpty()) {
            return failure(source, "command.bananaclaims.error.cannot_manage_named", claimName);
        }
        return show(source, claim.get());
    }

    private static int show(CommandSourceStack source, Claim claim) {
        ClaimBlueMapStyle style = claim.getBlueMapStyle();
        return success(
                source,
                "command.bananaclaims.bluemap.info",
                claim.getName(),
                style.getFillColor(),
                style.getFillOpacity(),
                style.getLineColor(),
                style.getLineOpacity(),
                style.getLineWidth()
        );
    }

    private static int setColor(
            CommandSourceStack source,
            String claimName,
            String color,
            boolean fill
    ) throws CommandSyntaxException {
        Optional<Claim> optionalClaim = editableClaim(source, claimName);
        if (optionalClaim.isEmpty()) {
            return 0;
        }
        Claim claim = optionalClaim.get();
        boolean valid = fill
                ? claim.getBlueMapStyle().setFillColor(color)
                : claim.getBlueMapStyle().setLineColor(color);
        if (!valid) {
            return failure(source, "command.bananaclaims.bluemap.invalid_color", color);
        }
        Bananaclaims.CLAIM_MANAGER.markClaimUpdated(claim);
        return success(
                source,
                fill
                        ? "command.bananaclaims.bluemap.fill_set"
                        : "command.bananaclaims.bluemap.line_set",
                claim.getName(),
                fill
                        ? claim.getBlueMapStyle().getFillColor()
                        : claim.getBlueMapStyle().getLineColor()
        );
    }

    private static int setOpacity(
            CommandSourceStack source,
            String claimName,
            float opacity,
            boolean fill
    ) throws CommandSyntaxException {
        Optional<Claim> optionalClaim = editableClaim(source, claimName);
        if (optionalClaim.isEmpty()) {
            return 0;
        }
        Claim claim = optionalClaim.get();
        boolean valid = fill
                ? claim.getBlueMapStyle().setFillOpacity(opacity)
                : claim.getBlueMapStyle().setLineOpacity(opacity);
        if (!valid) {
            return failure(source, "command.bananaclaims.bluemap.invalid_opacity");
        }
        Bananaclaims.CLAIM_MANAGER.markClaimUpdated(claim);
        return success(
                source,
                fill
                        ? "command.bananaclaims.bluemap.fill_opacity_set"
                        : "command.bananaclaims.bluemap.line_opacity_set",
                claim.getName(),
                opacity
        );
    }

    private static int setLineWidth(
            CommandSourceStack source,
            String claimName,
            int width
    ) throws CommandSyntaxException {
        Optional<Claim> optionalClaim = editableClaim(source, claimName);
        if (optionalClaim.isEmpty()) {
            return 0;
        }
        Claim claim = optionalClaim.get();
        if (!claim.getBlueMapStyle().setLineWidth(width)) {
            return failure(source, "command.bananaclaims.bluemap.invalid_width");
        }
        Bananaclaims.CLAIM_MANAGER.markClaimUpdated(claim);
        return success(
                source,
                "command.bananaclaims.bluemap.width_set",
                claim.getName(),
                width
        );
    }

    private static int reset(CommandSourceStack source, String claimName)
            throws CommandSyntaxException {
        Optional<Claim> optionalClaim = editableClaim(source, claimName);
        if (optionalClaim.isEmpty()) {
            return 0;
        }
        Claim claim = optionalClaim.get();
        claim.getBlueMapStyle().reset();
        Bananaclaims.CLAIM_MANAGER.markClaimUpdated(claim);
        return success(source, "command.bananaclaims.bluemap.reset", claim.getName());
    }

    private static Optional<Claim> editableClaim(
            CommandSourceStack source,
            String claimName
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<Claim> claim = ClaimResolver.findManagedByName(player.getUUID(), claimName);
        if (claim.isEmpty()) {
            source.sendFailure(BananaClaimsMessages.text(
                    "command.bananaclaims.error.cannot_manage_named",
                    claimName
            ));
            return Optional.empty();
        }
        if (!claim.get().canEditAppearance(player.getUUID())) {
            source.sendFailure(BananaClaimsMessages.text(
                    "command.bananaclaims.bluemap.not_authorized",
                    claimName
            ));
            return Optional.empty();
        }
        return claim;
    }

    private static int success(CommandSourceStack source, String key, Object... args) {
        source.sendSuccess(() -> BananaClaimsMessages.text(key, args), false);
        return 1;
    }

    private static int failure(CommandSourceStack source, String key, Object... args) {
        source.sendFailure(BananaClaimsMessages.text(key, args));
        return 0;
    }
}
