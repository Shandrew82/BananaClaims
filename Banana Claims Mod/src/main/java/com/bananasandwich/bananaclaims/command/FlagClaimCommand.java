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
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class FlagClaimCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("flag")
                .then(Commands.argument("claim", StringArgumentType.word())
                        .suggests(ClaimSuggestions.MANAGED_CLAIMS)
                        .then(Commands.argument("flag", StringArgumentType.word())
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> updateFlag(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "claim"),
                                                StringArgumentType.getString(context, "flag"),
                                                BoolArgumentType.getBool(context, "value")
                                        ))
                                )
                        )
                );
    }

    private static int updateFlag(
            CommandSourceStack source,
            String claimName,
            String flagName,
            boolean value
    ) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("Only players can edit claim flags."));
            return 0;
        }

        Optional<Claim> optionalClaim =
                ClaimResolver.findManagedByName(
                        player.getUUID(),
                        claimName
                );

        if (optionalClaim.isEmpty()) {
            source.sendFailure(Component.literal(
                    "You cannot manage a claim named \""
                            + claimName
                            + "\"."
            ));
            return 0;
        }

        Claim claim = optionalClaim.get();

        if (!claim.canEditFlags(player.getUUID())) {
            source.sendFailure(Component.literal(
                    "You cannot edit flags for claim \""
                            + claim.getName()
                            + "\"."
            ));
            return 0;
        }

        if (!setFlag(claim.getFlags(), flagName, value)) {
            source.sendFailure(Component.literal("Unknown flag: " + flagName));
            return 0;
        }

        Bananaclaims.CLAIM_MANAGER.saveClaims();

        source.sendSuccess(
                () -> Component.literal(
                        "Set " + flagName + " to " + value
                                + " for claim \"" + claim.getName() + "\"."
                ),
                false
        );

        return 1;
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
            default -> {
                return false;
            }
        }

        return true;
    }
}