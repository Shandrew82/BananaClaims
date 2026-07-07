package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.Optional;

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

                        .then(Commands.literal("create")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            String name = StringArgumentType.getString(context, "name");

                                            ChunkPos chunkPos = player.chunkPosition();

                                            Claim claim = new Claim(
                                                    name,
                                                    player.getUUID(),
                                                    player.getName().getString(),
                                                    player.level().dimension().toString(),
                                                    chunkPos.x(),
                                                    chunkPos.z()
                                            );

                                            boolean created = Bananaclaims.CLAIM_MANAGER.addClaim(claim);

                                            if (!created) {
                                                context.getSource().sendFailure(
                                                        Component.literal("This chunk is already claimed.")
                                                );
                                                return 0;
                                            }

                                            Bananaclaims.CLAIM_STORAGE.saveClaims(Bananaclaims.CLAIM_MANAGER.getAllClaims());

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Claim \"" + name + "\" created successfully."),
                                                    false
                                            );

                                            return 1;
                                        })
                                )
                        )

                        .then(Commands.literal("info")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    ChunkPos chunkPos = player.chunkPosition();
                                    String dimension = player.level().dimension().toString();

                                    Optional<Claim> optionalClaim = Bananaclaims.CLAIM_MANAGER.getClaimAt(
                                            dimension,
                                            chunkPos.x(),
                                            chunkPos.z()
                                    );

                                    if (optionalClaim.isEmpty()) {
                                        context.getSource().sendSuccess(
                                                () -> Component.literal("You are standing in Wilderness."),
                                                false
                                        );
                                        return 1;
                                    }

                                    Claim claim = optionalClaim.get();

                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "Claim: " + claim.getName()
                                                            + "\nOwner: " + claim.getOwnerName()
                                                            + "\nDescription: " + (claim.getDescription().isBlank() ? "No description." : claim.getDescription())
                                                            + "\nDimension: " + claim.getDimension()
                                                            + "\nChunk: " + claim.getChunkX() + ", " + claim.getChunkZ()
                                            ),
                                            false
                                    );

                                    return 1;
                                })
                        )
        );
    }
}