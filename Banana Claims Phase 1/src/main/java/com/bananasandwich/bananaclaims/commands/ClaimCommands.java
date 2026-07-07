package com.bananasandwich.bananaclaims.commands;

import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.claim.ClaimManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

public final class ClaimCommands {
    private ClaimCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("claim")
                        .then(Commands.literal("create")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> create(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("delete")
                                .executes(ctx -> delete(ctx.getSource().getPlayerOrException())))
                        .then(Commands.literal("info")
                                .executes(ctx -> info(ctx.getSource().getPlayerOrException())))
                        .then(Commands.literal("list")
                                .executes(ctx -> list(ctx.getSource().getPlayerOrException())))
                        .then(Commands.literal("rename")
                                .then(Commands.argument("oldName", StringArgumentType.word())
                                        .then(Commands.argument("newName", StringArgumentType.word())
                                                .executes(ctx -> rename(
                                                        ctx.getSource().getPlayerOrException(),
                                                        StringArgumentType.getString(ctx, "oldName"),
                                                        StringArgumentType.getString(ctx, "newName")
                                                )))))
                        .then(Commands.literal("description")
                                .then(Commands.argument("claim", StringArgumentType.word())
                                        .then(Commands.argument("description", StringArgumentType.greedyString())
                                                .executes(ctx -> description(
                                                        ctx.getSource().getPlayerOrException(),
                                                        StringArgumentType.getString(ctx, "claim"),
                                                        StringArgumentType.getString(ctx, "description")
                                                )))))
                        .then(Commands.literal("reload")
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> reload(ctx.getSource().getPlayerOrException())))
        ));
    }

    private static int create(ServerPlayer player, String name) {
        Claim claim = ClaimManager.createClaim(player, name);
        if (claim == null) {
            player.sendSystemMessage(Component.literal("This chunk is already claimed."));
            return 0;
        }
        player.sendSystemMessage(Component.literal("Created claim: " + claim.name));
        return 1;
    }

    private static int delete(ServerPlayer player) {
        if (!ClaimManager.deleteClaimAt(player)) {
            player.sendSystemMessage(Component.literal("You are not standing in a claim."));
            return 0;
        }
        player.sendSystemMessage(Component.literal("Deleted the claim you were standing in."));
        return 1;
    }

    private static int info(ServerPlayer player) {
        Optional<Claim> claim = ClaimManager.getClaimAt(player);
        if (claim.isEmpty()) {
            player.sendSystemMessage(Component.literal("You are not standing in a claim."));
            return 0;
        }
        Claim c = claim.get();
        player.sendSystemMessage(Component.literal("Claim: " + c.name));
        player.sendSystemMessage(Component.literal("Owner: " + c.ownerName));
        player.sendSystemMessage(Component.literal("Chunks: " + c.chunks.size()));
        if (c.description != null && !c.description.isBlank()) {
            player.sendSystemMessage(Component.literal("Description: " + c.description));
        }
        return 1;
    }

    private static int list(ServerPlayer player) {
        List<Claim> claims = ClaimManager.getClaimsFor(player);
        if (claims.isEmpty()) {
            player.sendSystemMessage(Component.literal("You do not own any claims yet."));
            return 0;
        }
        player.sendSystemMessage(Component.literal("Your claims:"));
        for (Claim claim : claims) {
            player.sendSystemMessage(Component.literal("- " + claim.name + " (" + claim.chunks.size() + " chunk)"));
        }
        return 1;
    }

    private static int rename(ServerPlayer player, String oldName, String newName) {
        if (!ClaimManager.renameClaim(player, oldName, newName)) {
            player.sendSystemMessage(Component.literal("Could not find one of your claims named: " + oldName));
            return 0;
        }
        player.sendSystemMessage(Component.literal("Renamed claim to: " + newName));
        return 1;
    }

    private static int description(ServerPlayer player, String claim, String description) {
        if (!ClaimManager.setDescription(player, claim, description)) {
            player.sendSystemMessage(Component.literal("Could not find one of your claims named: " + claim));
            return 0;
        }
        player.sendSystemMessage(Component.literal("Updated description for: " + claim));
        return 1;
    }

    private static int reload(ServerPlayer player) {
        ClaimManager.load();
        player.sendSystemMessage(Component.literal("Banana Claims reloaded."));
        return 1;
    }
}
