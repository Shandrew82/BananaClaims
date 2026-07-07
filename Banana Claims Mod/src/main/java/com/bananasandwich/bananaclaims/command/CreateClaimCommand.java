package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public class CreateClaimCommand {

    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("create")
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
                );
    }
}