package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.localization.BananaClaimsMessages;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
                                        BananaClaimsMessages.text("command.bananaclaims.error.chunk_claimed")
                                );
                                return 0;
                            }


                            context.getSource().sendSuccess(
                                    () -> BananaClaimsMessages.text("command.bananaclaims.create.success", name),
                                    false
                            );

                            return 1;
                        })
                );
    }
}