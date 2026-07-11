package com.bananasandwich.bananaclaims.command;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.invitation.ClaimInvitation;
import com.bananasandwich.bananaclaims.invitation.InvitationResult;
import com.bananasandwich.bananaclaims.localization.BananaClaimsMessages;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class InviteClaimCommand {

    private InviteClaimCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("invite")
                .then(Commands.literal("accept")
                        .then(Commands.argument("claim", StringArgumentType.greedyString())
                                .suggests(ClaimSuggestions.INCOMING_INVITE_CLAIMS)
                                .executes(context -> accept(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "claim")
                                ))))
                .then(Commands.literal("deny")
                        .then(Commands.argument("claim", StringArgumentType.greedyString())
                                .suggests(ClaimSuggestions.INCOMING_INVITE_CLAIMS)
                                .executes(context -> deny(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "claim")
                                ))))
                .then(Commands.literal("cancel")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(ClaimSuggestions.OUTGOING_INVITE_PLAYERS)
                                .executes(context -> cancelCurrent(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "player")
                                ))
                                .then(Commands.argument("claim", StringArgumentType.word())
                                        .suggests(ClaimSuggestions.MANAGED_CLAIMS)
                                        .executes(context -> cancelNamed(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "player"),
                                                StringArgumentType.getString(context, "claim")
                                        )))))
                .then(Commands.literal("list")
                        .executes(context -> list(context.getSource())))
                .then(Commands.literal("for")
                        .then(Commands.argument("claim", StringArgumentType.word())
                                .suggests(ClaimSuggestions.MANAGED_CLAIMS)
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests(ClaimSuggestions.ONLINE_PLAYERS)
                                        .executes(context -> createNamed(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "player"),
                                                StringArgumentType.getString(context, "claim")
                                        )))))
                .then(Commands.literal("cancel-for")
                        .then(Commands.argument("claim", StringArgumentType.word())
                                .suggests(ClaimSuggestions.MANAGED_CLAIMS)
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests(ClaimSuggestions.OUTGOING_INVITE_PLAYERS)
                                        .executes(context -> cancelNamed(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "player"),
                                                StringArgumentType.getString(context, "claim")
                                        )))))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(ClaimSuggestions.ONLINE_PLAYERS)
                        .executes(context -> createCurrent(
                                context.getSource(),
                                StringArgumentType.getString(context, "player")
                        ))
                        .then(Commands.argument("claim", StringArgumentType.word())
                                .suggests(ClaimSuggestions.MANAGED_CLAIMS)
                                .executes(context -> createNamed(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "player"),
                                        StringArgumentType.getString(context, "claim")
                                ))));
    }

    private static int createCurrent(CommandSourceStack source, String playerName)
            throws CommandSyntaxException {
        ServerPlayer inviter = source.getPlayerOrException();
        Optional<Claim> claim = ClaimResolver.findAtPlayer(inviter)
                .filter(value -> value.canEditMembers(inviter.getUUID()));
        if (claim.isEmpty()) {
            source.sendFailure(BananaClaimsMessages.text(
                    "command.bananaclaims.invite.no_managed_claim_here"
            ));
            return 0;
        }
        return create(source, inviter, playerName, claim.get());
    }

    private static int createNamed(
            CommandSourceStack source,
            String playerName,
            String claimName
    ) throws CommandSyntaxException {
        ServerPlayer inviter = source.getPlayerOrException();
        Optional<Claim> claim = ClaimResolver.findManagedByName(inviter.getUUID(), claimName);
        if (claim.isEmpty()) {
            source.sendFailure(BananaClaimsMessages.text(
                    "command.bananaclaims.error.cannot_manage_named",
                    claimName
            ));
            return 0;
        }
        return create(source, inviter, playerName, claim.get());
    }

    private static int create(
            CommandSourceStack source,
            ServerPlayer inviter,
            String playerName,
            Claim claim
    ) {
        ServerPlayer invitee = source.getServer().getPlayerList().getPlayerByName(playerName);
        if (invitee == null) {
            source.sendFailure(BananaClaimsMessages.text(
                    "command.bananaclaims.error.online_player_not_found",
                    playerName
            ));
            return 0;
        }

        InvitationResult result = Bananaclaims.INVITATION_MANAGER.create(
                inviter,
                invitee,
                claim
        );

        return switch (result) {
            case CREATED -> success(source, "command.bananaclaims.invite.sent", invitee.getScoreboardName(), claim.getName());
            case DISABLED -> failure(source, "command.bananaclaims.invite.disabled");
            case NOT_AUTHORIZED -> failure(source, "command.bananaclaims.invite.not_authorized", claim.getName());
            case CANNOT_INVITE_SELF -> failure(source, "command.bananaclaims.invite.self");
            case ALREADY_PARTICIPANT -> failure(source, "command.bananaclaims.invite.already_participant", invitee.getScoreboardName());
            case ALREADY_INVITED -> failure(source, "command.bananaclaims.invite.already_pending", invitee.getScoreboardName(), claim.getName());
            case CLAIM_LIMIT_REACHED -> failure(source, "command.bananaclaims.invite.limit", claim.getName());
            default -> failure(source, "command.bananaclaims.invite.failed");
        };
    }

    private static int accept(CommandSourceStack source, String selector)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<ClaimInvitation> invitation = Bananaclaims.INVITATION_MANAGER
                .findIncoming(player.getUUID(), selector);
        if (invitation.isEmpty()) {
            return failure(source, "command.bananaclaims.invite.not_found", selector);
        }

        Optional<Claim> claim = Bananaclaims.CLAIM_MANAGER
                .getClaimById(invitation.get().claimId());
        if (claim.isEmpty()) {
            return failure(source, "command.bananaclaims.error.no_claim_named", invitation.get().claimName());
        }

        InvitationResult result = Bananaclaims.INVITATION_MANAGER.accept(player, claim.get());
        return switch (result) {
            case ACCEPTED -> success(source, "command.bananaclaims.invite.accepted", claim.get().getName());
            case INVITATION_EXPIRED -> failure(source, "command.bananaclaims.invite.expired", claim.get().getName());
            case ALREADY_PARTICIPANT -> failure(source, "command.bananaclaims.invite.accept_already_member", claim.get().getName());
            default -> failure(source, "command.bananaclaims.invite.not_found", invitation.get().selector());
        };
    }

    private static int deny(CommandSourceStack source, String selector)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<ClaimInvitation> invitation = Bananaclaims.INVITATION_MANAGER
                .findIncoming(player.getUUID(), selector);
        if (invitation.isEmpty()) {
            return failure(source, "command.bananaclaims.invite.not_found", selector);
        }

        Optional<Claim> claim = Bananaclaims.CLAIM_MANAGER
                .getClaimById(invitation.get().claimId());
        if (claim.isEmpty()) {
            return failure(source, "command.bananaclaims.error.no_claim_named", invitation.get().claimName());
        }

        InvitationResult result = Bananaclaims.INVITATION_MANAGER.deny(player, claim.get());
        return result == InvitationResult.DENIED
                ? success(source, "command.bananaclaims.invite.denied", claim.get().getName())
                : failure(source, "command.bananaclaims.invite.not_found", invitation.get().selector());
    }

    private static int cancelCurrent(CommandSourceStack source, String playerName)
            throws CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        Optional<Claim> claim = ClaimResolver.findAtPlayer(actor)
                .filter(value -> value.canEditMembers(actor.getUUID()));
        if (claim.isEmpty()) {
            return failure(source, "command.bananaclaims.invite.no_managed_claim_here");
        }
        return cancel(source, actor, playerName, claim.get());
    }

    private static int cancelNamed(
            CommandSourceStack source,
            String playerName,
            String claimName
    ) throws CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        Optional<Claim> claim = ClaimResolver.findManagedByName(actor.getUUID(), claimName);
        if (claim.isEmpty()) {
            return failure(source, "command.bananaclaims.error.cannot_manage_named", claimName);
        }
        return cancel(source, actor, playerName, claim.get());
    }

    private static int cancel(
            CommandSourceStack source,
            ServerPlayer actor,
            String playerName,
            Claim claim
    ) {
        Optional<ClaimInvitation> invitation = Bananaclaims.INVITATION_MANAGER
                .getForClaim(claim.getClaimId())
                .stream()
                .filter(value -> value.inviteeName().equalsIgnoreCase(playerName))
                .findFirst();

        if (invitation.isEmpty()) {
            return failure(source, "command.bananaclaims.invite.cancel_not_found", playerName, claim.getName());
        }

        InvitationResult result = Bananaclaims.INVITATION_MANAGER.cancel(
                actor,
                claim,
                invitation.get().inviteeUuid()
        );
        return result == InvitationResult.CANCELLED
                ? success(source, "command.bananaclaims.invite.cancelled", playerName, claim.getName())
                : failure(source, "command.bananaclaims.invite.failed");
    }

    private static int list(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        List<ClaimInvitation> incoming = Bananaclaims.INVITATION_MANAGER.getIncoming(player.getUUID());
        List<ClaimInvitation> outgoing = Bananaclaims.INVITATION_MANAGER.getOutgoing(player.getUUID());

        source.sendSuccess(() -> BananaClaimsMessages.text(
                "command.bananaclaims.invite.list_header",
                incoming.size(),
                outgoing.size()
        ), false);

        for (ClaimInvitation invitation : incoming) {
            source.sendSuccess(() -> BananaClaimsMessages.text(
                    "command.bananaclaims.invite.list_incoming",
                    invitation.claimName(),
                    invitation.inviterName(),
                    secondsRemaining(invitation)
            ), false);
        }
        for (ClaimInvitation invitation : outgoing) {
            source.sendSuccess(() -> BananaClaimsMessages.text(
                    "command.bananaclaims.invite.list_outgoing",
                    invitation.claimName(),
                    invitation.inviteeName(),
                    secondsRemaining(invitation)
            ), false);
        }
        if (incoming.isEmpty() && outgoing.isEmpty()) {
            source.sendSuccess(() -> BananaClaimsMessages.text(
                    "command.bananaclaims.invite.list_empty"
            ), false);
        }
        return 1;
    }

    private static long secondsRemaining(ClaimInvitation invitation) {
        return Math.max(0L, (invitation.expiresAtMillis() - System.currentTimeMillis() + 999L) / 1000L);
    }

    private static int success(CommandSourceStack source, String key, Object... arguments) {
        source.sendSuccess(() -> BananaClaimsMessages.text(key, arguments), false);
        return 1;
    }

    private static int failure(CommandSourceStack source, String key, Object... arguments) {
        source.sendFailure(BananaClaimsMessages.text(key, arguments));
        return 0;
    }
}
