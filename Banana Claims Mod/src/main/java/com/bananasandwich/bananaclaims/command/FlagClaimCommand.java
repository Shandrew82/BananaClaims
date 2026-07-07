package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.claim.ClaimFlags;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public class FlagClaimCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("flag")
                .then(Commands.argument("claim", StringArgumentType.word())
                        .then(Commands.argument("flag", StringArgumentType.word())
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> {
                                            String claimName = StringArgumentType.getString(context, "claim");
                                            String flagName = StringArgumentType.getString(context, "flag");
                                            boolean value = BoolArgumentType.getBool(context, "value");

                                            Optional<Claim> optionalClaim = Bananaclaims.CLAIM_MANAGER.getAllClaims().stream()
                                                    .filter(claim -> claim.getName().equalsIgnoreCase(claimName))
                                                    .findFirst();

                                            if (optionalClaim.isEmpty()) {
                                                context.getSource().sendFailure(Component.literal("No claim found named \"" + claimName + "\"."));
                                                return 0;
                                            }

                                            Claim claim = optionalClaim.get();

                                            boolean updated = setFlag(claim.getFlags(), flagName, value);

                                            if (!updated) {
                                                context.getSource().sendFailure(Component.literal("Unknown flag: " + flagName));
                                                return 0;
                                            }

                                            Bananaclaims.CLAIM_MANAGER.saveClaims();

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Set " + flagName + " to " + value + " for claim \"" + claim.getName() + "\"."),
                                                    false
                                            );

                                            return 1;
                                        })
                                )
                        )
                );
    }

    private static boolean setFlag(ClaimFlags flags, String flagName, boolean value) {
        switch (flagName.toLowerCase()) {
            case "breakblocks" -> flags.setBreakBlocks(value);
            case "placeblocks" -> flags.setPlaceBlocks(value);
            case "interact" -> flags.setInteract(value);
            case "containers" -> flags.setContainers(value);
            case "entities" -> flags.setEntities(value);
            case "pvp" -> flags.setPvp(value);
            case "explosions" -> flags.setExplosions(value);
            case "firespread" -> flags.setFireSpread(value);
            case "mobgriefing" -> flags.setMobGriefing(value);
            default -> {
                return false;
            }
        }

        return true;
    }
}