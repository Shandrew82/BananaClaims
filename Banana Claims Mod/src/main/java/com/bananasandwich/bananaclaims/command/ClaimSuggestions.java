package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.claim.ClaimMember;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;
import java.util.Optional;

public final class ClaimSuggestions {

    public static final SuggestionProvider<CommandSourceStack> ALL_CLAIMS =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    Bananaclaims.CLAIM_MANAGER
                            .getAllClaims()
                            .stream()
                            .map(Claim::getName)
                            .filter(name -> name != null && !name.isBlank())
                            .sorted(String.CASE_INSENSITIVE_ORDER),
                    builder
            );

    public static final SuggestionProvider<CommandSourceStack> OWNED_CLAIMS =
            (context, builder) -> {
                try {
                    List<String> claimNames =
                            Bananaclaims.CLAIM_MANAGER
                                    .getClaimsForOwner(
                                            context.getSource()
                                                    .getPlayerOrException()
                                                    .getUUID()
                                    )
                                    .stream()
                                    .map(Claim::getName)
                                    .filter(name ->
                                            name != null
                                                    && !name.isBlank()
                                    )
                                    .sorted(String.CASE_INSENSITIVE_ORDER)
                                    .toList();

                    return SharedSuggestionProvider.suggest(
                            claimNames,
                            builder
                    );
                } catch (Exception exception) {
                    return builder.buildFuture();
                }
            };

    public static final SuggestionProvider<CommandSourceStack> MANAGED_CLAIMS =
            (context, builder) -> {
                try {
                    java.util.UUID playerUuid =
                            context.getSource()
                                    .getPlayerOrException()
                                    .getUUID();

                    List<String> claimNames =
                            Bananaclaims.CLAIM_MANAGER
                                    .getAllClaims()
                                    .stream()
                                    .filter(claim ->
                                            claim.canManage(playerUuid)
                                    )
                                    .map(Claim::getName)
                                    .filter(name ->
                                            name != null
                                                    && !name.isBlank()
                                    )
                                    .sorted(String.CASE_INSENSITIVE_ORDER)
                                    .toList();

                    return SharedSuggestionProvider.suggest(
                            claimNames,
                            builder
                    );
                } catch (Exception exception) {
                    return builder.buildFuture();
                }
            };

    public static final SuggestionProvider<CommandSourceStack> ONLINE_PLAYERS =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    context.getSource()
                            .getServer()
                            .getPlayerList()
                            .getPlayers()
                            .stream()
                            .map(player ->
                                    player.getName().getString()
                            )
                            .sorted(String.CASE_INSENSITIVE_ORDER),
                    builder
            );

    public static final SuggestionProvider<CommandSourceStack> CURRENT_CLAIM_MEMBERS =
            (context, builder) -> {
                Optional<Claim> optionalClaim =
                        ClaimResolver.findAtSource(
                                context.getSource()
                        );

                if (optionalClaim.isEmpty()) {
                    return builder.buildFuture();
                }

                return suggestMembers(
                        optionalClaim.get(),
                        builder
                );
            };

    public static final SuggestionProvider<CommandSourceStack> NAMED_CLAIM_MEMBERS =
            (context, builder) -> {
                String claimName = getArgumentSafely(
                        context,
                        "claim"
                );

                Optional<Claim> optionalClaim =
                        ClaimResolver.findByName(claimName);

                if (optionalClaim.isEmpty()) {
                    return builder.buildFuture();
                }

                return suggestMembers(
                        optionalClaim.get(),
                        builder
                );
            };

    public static final SuggestionProvider<CommandSourceStack> POPUP_MODES =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    List.of(
                            "ACTIONBAR",
                            "TITLE",
                            "CHAT"
                    ),
                    builder
            );

    public static final SuggestionProvider<CommandSourceStack> SOUNDS =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    BuiltInRegistries.SOUND_EVENT
                            .keySet()
                            .stream()
                            .map(Object::toString)
                            .sorted(),
                    builder
            );

    private ClaimSuggestions() {
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestMembers(
            Claim claim,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggest(
                claim.getMembers()
                        .stream()
                        .map(ClaimMember::getName)
                        .filter(name ->
                                name != null
                                        && !name.isBlank()
                        )
                        .sorted(String.CASE_INSENSITIVE_ORDER),
                builder
        );
    }

    private static String getArgumentSafely(
            CommandContext<CommandSourceStack> context,
            String argumentName
    ) {
        try {
            return context.getArgument(
                    argumentName,
                    String.class
            );
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }
}