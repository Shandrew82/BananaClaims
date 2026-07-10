package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.claim.PopupDisplayMode;
import com.bananasandwich.bananaclaims.notification.PopupRenderer;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class PopupClaimCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("popup")
                .then(Commands.argument(
                                        "claim",
                                        StringArgumentType.word()
                                )
                                .suggests(ClaimSuggestions.ALL_CLAIMS)

                                .then(Commands.literal("preview")
                                        .then(Commands.literal("enter")
                                                .executes(context -> previewEnter(
                                                        context.getSource(),
                                                        StringArgumentType.getString(
                                                                context,
                                                                "claim"
                                                        )
                                                ))
                                        )
                                        .then(Commands.literal("leave")
                                                .executes(context -> previewLeave(
                                                        context.getSource(),
                                                        StringArgumentType.getString(
                                                                context,
                                                                "claim"
                                                        )
                                                ))
                                        )
                                )

                                .then(Commands.literal("set")
                                        .then(Commands.literal("mode")
                                                .then(Commands.argument(
                                                                        "mode",
                                                                        StringArgumentType.word()
                                                                )
                                                                .suggests(
                                                                        ClaimSuggestions.POPUP_MODES
                                                                )
                                                                .executes(context -> setMode(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "claim"
                                                                        ),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "mode"
                                                                        )
                                                                ))
                                                )
                                        )

                                        .then(Commands.literal("enterTitle")
                                                .then(Commands.argument(
                                                                        "text",
                                                                        StringArgumentType.greedyString()
                                                                )
                                                                .executes(context -> setEnterTitle(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "claim"
                                                                        ),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "text"
                                                                        )
                                                                ))
                                                )
                                        )

                                        .then(Commands.literal("enterSubtitle")
                                                .then(Commands.argument(
                                                                        "text",
                                                                        StringArgumentType.greedyString()
                                                                )
                                                                .executes(context -> setEnterSubtitle(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "claim"
                                                                        ),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "text"
                                                                        )
                                                                ))
                                                )
                                        )

                                        .then(Commands.literal("leaveTitle")
                                                .then(Commands.argument(
                                                                        "text",
                                                                        StringArgumentType.greedyString()
                                                                )
                                                                .executes(context -> setLeaveTitle(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "claim"
                                                                        ),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "text"
                                                                        )
                                                                ))
                                                )
                                        )

                                        .then(Commands.literal("leaveSubtitle")
                                                .then(Commands.argument(
                                                                        "text",
                                                                        StringArgumentType.greedyString()
                                                                )
                                                                .executes(context -> setLeaveSubtitle(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "claim"
                                                                        ),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "text"
                                                                        )
                                                                ))
                                                )
                                        )

                                        .then(Commands.literal("enterSound")
                                                .then(Commands.argument(
                                                                        "sound",
                                                                        StringArgumentType.greedyString()
                                                                )
                                                                .suggests(
                                                                        ClaimSuggestions.SOUNDS
                                                                )
                                                                .executes(context -> setEnterSound(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "claim"
                                                                        ),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "sound"
                                                                        )
                                                                ))
                                                )
                                        )

                                        .then(Commands.literal("leaveSound")
                                                .then(Commands.argument(
                                                                        "sound",
                                                                        StringArgumentType.greedyString()
                                                                )
                                                                .suggests(
                                                                        ClaimSuggestions.SOUNDS
                                                                )
                                                                .executes(context -> setLeaveSound(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "claim"
                                                                        ),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "sound"
                                                                        )
                                                                ))
                                                )
                                        )
                                )
                );
    }

    private static int previewEnter(
            CommandSourceStack source,
            String claimName
    ) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(
                    Component.literal(
                            "Only players can preview claim popups."
                    )
            );

            return 0;
        }

        Optional<Claim> optionalClaim =
                ClaimResolver.findByName(claimName);

        if (optionalClaim.isEmpty()) {
            source.sendFailure(
                    Component.literal(
                            "No claim found named \""
                                    + claimName
                                    + "\"."
                    )
            );

            return 0;
        }

        PopupRenderer.showEnter(
                player,
                optionalClaim.get()
        );

        return 1;
    }

    private static int previewLeave(
            CommandSourceStack source,
            String claimName
    ) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(
                    Component.literal(
                            "Only players can preview claim popups."
                    )
            );

            return 0;
        }

        Optional<Claim> optionalClaim =
                ClaimResolver.findByName(claimName);

        if (optionalClaim.isEmpty()) {
            source.sendFailure(
                    Component.literal(
                            "No claim found named \""
                                    + claimName
                                    + "\"."
                    )
            );

            return 0;
        }

        PopupRenderer.showLeave(
                player,
                optionalClaim.get()
        );

        return 1;
    }

    private static Optional<Claim> findEditableClaim(
            CommandSourceStack source,
            String claimName
    ) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(
                    Component.literal(
                            "Only players can edit claim popups."
                    )
            );
            return Optional.empty();
        }

        Optional<Claim> optionalClaim =
                ClaimResolver.findManagedByName(
                        player.getUUID(),
                        claimName
                );

        if (optionalClaim.isEmpty()) {
            source.sendFailure(
                    Component.literal(
                            "You cannot manage a claim named \""
                                    + claimName
                                    + "\"."
                    )
            );
            return Optional.empty();
        }

        Claim claim = optionalClaim.get();

        if (!claim.canEditPopup(player.getUUID())) {
            source.sendFailure(
                    Component.literal(
                            "You cannot edit popup settings for claim \""
                                    + claim.getName()
                                    + "\"."
                    )
            );
            return Optional.empty();
        }

        return optionalClaim;
    }

    private static int setMode(
            CommandSourceStack source,
            String claimName,
            String modeText
    ) {
        Optional<Claim> optionalClaim =
                findEditableClaim(source, claimName);

        if (optionalClaim.isEmpty()) {
            return 0;
        }

        PopupDisplayMode mode;

        try {
            mode = PopupDisplayMode.valueOf(
                    modeText.toUpperCase()
            );
        } catch (IllegalArgumentException exception) {
            source.sendFailure(
                    Component.literal(
                            "Invalid popup mode. Use ACTIONBAR, TITLE, or CHAT."
                    )
            );

            return 0;
        }

        Claim claim = optionalClaim.get();

        claim.getPopupSettings().setDisplayMode(mode);
        Bananaclaims.CLAIM_MANAGER.saveClaims();

        source.sendSuccess(
                () -> Component.literal(
                        "Set popup mode for \""
                                + claim.getName()
                                + "\" to "
                                + mode
                                + "."
                ),
                false
        );

        return 1;
    }

    private static int setEnterTitle(
            CommandSourceStack source,
            String claimName,
            String text
    ) {
        Optional<Claim> optionalClaim =
                findEditableClaim(source, claimName);

        if (optionalClaim.isEmpty()) {
            return 0;
        }

        Claim claim = optionalClaim.get();

        claim.getPopupSettings().setEnterTitle(text);
        Bananaclaims.CLAIM_MANAGER.saveClaims();

        source.sendSuccess(
                () -> Component.literal(
                        "Set enter title for \""
                                + claim.getName()
                                + "\"."
                ),
                false
        );

        return 1;
    }

    private static int setEnterSubtitle(
            CommandSourceStack source,
            String claimName,
            String text
    ) {
        Optional<Claim> optionalClaim =
                findEditableClaim(source, claimName);

        if (optionalClaim.isEmpty()) {
            return 0;
        }

        Claim claim = optionalClaim.get();

        claim.getPopupSettings().setEnterSubtitle(text);
        Bananaclaims.CLAIM_MANAGER.saveClaims();

        source.sendSuccess(
                () -> Component.literal(
                        "Set enter subtitle for \""
                                + claim.getName()
                                + "\"."
                ),
                false
        );

        return 1;
    }

    private static int setLeaveTitle(
            CommandSourceStack source,
            String claimName,
            String text
    ) {
        Optional<Claim> optionalClaim =
                findEditableClaim(source, claimName);

        if (optionalClaim.isEmpty()) {
            return 0;
        }

        Claim claim = optionalClaim.get();

        claim.getPopupSettings().setLeaveTitle(text);
        Bananaclaims.CLAIM_MANAGER.saveClaims();

        source.sendSuccess(
                () -> Component.literal(
                        "Set leave title for \""
                                + claim.getName()
                                + "\"."
                ),
                false
        );

        return 1;
    }

    private static int setLeaveSubtitle(
            CommandSourceStack source,
            String claimName,
            String text
    ) {
        Optional<Claim> optionalClaim =
                findEditableClaim(source, claimName);

        if (optionalClaim.isEmpty()) {
            return 0;
        }

        Claim claim = optionalClaim.get();

        claim.getPopupSettings().setLeaveSubtitle(text);
        Bananaclaims.CLAIM_MANAGER.saveClaims();

        source.sendSuccess(
                () -> Component.literal(
                        "Set leave subtitle for \""
                                + claim.getName()
                                + "\"."
                ),
                false
        );

        return 1;
    }

    private static int setEnterSound(
            CommandSourceStack source,
            String claimName,
            String sound
    ) {
        Optional<Claim> optionalClaim =
                findEditableClaim(source, claimName);

        if (optionalClaim.isEmpty()) {
            return 0;
        }

        Claim claim = optionalClaim.get();

        claim.getPopupSettings().setEnterSound(
                normalizeSound(sound)
        );

        Bananaclaims.CLAIM_MANAGER.saveClaims();

        String savedSound =
                claim.getPopupSettings().getEnterSound();

        source.sendSuccess(
                () -> Component.literal(
                        savedSound.isBlank()
                                ? "Disabled enter sound for \""
                                  + claim.getName()
                                  + "\"."
                                : "Set enter sound for \""
                                  + claim.getName()
                                  + "\" to \""
                                  + savedSound
                                  + "\"."
                ),
                false
        );

        return 1;
    }

    private static int setLeaveSound(
            CommandSourceStack source,
            String claimName,
            String sound
    ) {
        Optional<Claim> optionalClaim =
                findEditableClaim(source, claimName);

        if (optionalClaim.isEmpty()) {
            return 0;
        }

        Claim claim = optionalClaim.get();

        claim.getPopupSettings().setLeaveSound(
                normalizeSound(sound)
        );

        Bananaclaims.CLAIM_MANAGER.saveClaims();

        String savedSound =
                claim.getPopupSettings().getLeaveSound();

        source.sendSuccess(
                () -> Component.literal(
                        savedSound.isBlank()
                                ? "Disabled leave sound for \""
                                  + claim.getName()
                                  + "\"."
                                : "Set leave sound for \""
                                  + claim.getName()
                                  + "\" to \""
                                  + savedSound
                                  + "\"."
                ),
                false
        );

        return 1;
    }

    private static String normalizeSound(String sound) {
        if (sound == null) {
            return "";
        }

        String trimmedSound = sound.trim();

        if (trimmedSound.equalsIgnoreCase("none")
                || trimmedSound.equalsIgnoreCase("clear")
                || trimmedSound.equalsIgnoreCase("off")
                || trimmedSound.equalsIgnoreCase("disable")
                || trimmedSound.equalsIgnoreCase("disabled")) {
            return "";
        }

        return trimmedSound;
    }
}