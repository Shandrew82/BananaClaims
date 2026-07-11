package com.bananasandwich.bananaclaims.invitation;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.claim.ClaimMutationResult;
import com.bananasandwich.bananaclaims.claim.ClaimRole;
import com.bananasandwich.bananaclaims.claim.event.ClaimChangeEvent;
import com.bananasandwich.bananaclaims.claim.event.ClaimChangeType;
import com.bananasandwich.bananaclaims.config.BananaClaimsConfigManager;
import com.bananasandwich.bananaclaims.localization.BananaClaimsMessages;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import com.bananasandwich.bananaclaims.book.BookComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Session-scoped claim invitation service.
 *
 * <p>Invitations intentionally do not persist through a server restart. This
 * prevents stale approvals from granting access long after their original
 * context has changed.</p>
 */
public final class ClaimInvitationManager {

    private final BananaClaimsConfigManager configManager;
    private final Map<UUID, ClaimInvitation> invitations = new LinkedHashMap<>();

    private MinecraftServer server;
    private long nextExpiryCheckTick;
    private boolean registered;

    public ClaimInvitationManager(BananaClaimsConfigManager configManager) {
        this.configManager = Objects.requireNonNull(configManager, "configManager");
    }

    public void register() {
        if (registered) {
            return;
        }
        registered = true;

        ServerLifecycleEvents.SERVER_STOPPING.register(stoppingServer -> {
            invitations.clear();
            server = null;
        });
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
        Bananaclaims.CLAIM_MANAGER.addChangeListener(this::onClaimChanged);
    }

    public synchronized InvitationResult create(
            ServerPlayer inviter,
            ServerPlayer invitee,
            Claim claim
    ) {
        if (!configManager.areInvitationsEnabled()) {
            return InvitationResult.DISABLED;
        }
        server = inviter == null ? server : inviter.level().getServer();
        if (inviter == null || invitee == null) {
            return InvitationResult.INVALID_PLAYER;
        }
        if (claim == null || Bananaclaims.CLAIM_MANAGER.getClaimById(claim.getClaimId()).isEmpty()) {
            return InvitationResult.CLAIM_NOT_FOUND;
        }
        if (!claim.canEditMembers(inviter.getUUID())) {
            return InvitationResult.NOT_AUTHORIZED;
        }
        if (inviter.getUUID().equals(invitee.getUUID())) {
            return InvitationResult.CANNOT_INVITE_SELF;
        }
        if (claim.getRole(invitee.getUUID()) != ClaimRole.NONE) {
            return InvitationResult.ALREADY_PARTICIPANT;
        }

        pruneExpired(System.currentTimeMillis(), false);

        boolean duplicate = invitations.values().stream()
                .anyMatch(invitation ->
                        invitation.claimId().equals(claim.getClaimId())
                                && invitation.inviteeUuid().equals(invitee.getUUID())
                );
        if (duplicate) {
            return InvitationResult.ALREADY_INVITED;
        }

        long pendingForClaim = invitations.values().stream()
                .filter(invitation -> invitation.claimId().equals(claim.getClaimId()))
                .count();
        if (pendingForClaim >= configManager.getMaxPendingInvitationsPerClaim()) {
            return InvitationResult.CLAIM_LIMIT_REACHED;
        }

        long now = System.currentTimeMillis();
        long expiresAt = now + configManager.getInvitationExpirationSeconds() * 1000L;

        ClaimInvitation invitation = new ClaimInvitation(
                UUID.randomUUID(),
                claim.getClaimId(),
                claim.getName(),
                inviter.getUUID(),
                inviter.getScoreboardName(),
                invitee.getUUID(),
                invitee.getScoreboardName(),
                now,
                expiresAt
        );
        invitations.put(invitation.invitationId(), invitation);

        notifyInvitee(invitee, invitation);
        return InvitationResult.CREATED;
    }

    public synchronized InvitationResult accept(
            ServerPlayer invitee,
            Claim claim
    ) {
        if (invitee == null || claim == null) {
            return InvitationResult.INVALID_PLAYER;
        }

        Optional<ClaimInvitation> optionalInvitation = findPending(
                claim.getClaimId(),
                invitee.getUUID()
        );
        if (optionalInvitation.isEmpty()) {
            return InvitationResult.INVITATION_NOT_FOUND;
        }

        ClaimInvitation invitation = optionalInvitation.get();
        if (invitation.isExpired(System.currentTimeMillis())) {
            invitations.remove(invitation.invitationId());
            return InvitationResult.INVITATION_EXPIRED;
        }

        Optional<Claim> currentClaim = Bananaclaims.CLAIM_MANAGER.getClaimById(invitation.claimId());
        if (currentClaim.isEmpty()) {
            invitations.remove(invitation.invitationId());
            return InvitationResult.CLAIM_NOT_FOUND;
        }

        ClaimMutationResult result = Bananaclaims.CLAIM_MANAGER.acceptInvitation(
                currentClaim.get(),
                invitee.getUUID(),
                invitee.getScoreboardName()
        );

        if (result != ClaimMutationResult.MEMBER_ADDED) {
            if (result == ClaimMutationResult.PLAYER_IS_MEMBER
                    || result == ClaimMutationResult.PLAYER_IS_SUBOWNER
                    || result == ClaimMutationResult.PLAYER_IS_OWNER) {
                removeForClaimAndInvitee(invitation.claimId(), invitee.getUUID());
                return InvitationResult.ALREADY_PARTICIPANT;
            }
            return InvitationResult.NO_CHANGE;
        }

        removeForClaimAndInvitee(invitation.claimId(), invitee.getUUID());
        notifyInviter(invitation, BananaClaimsMessages.text(
                "command.bananaclaims.invite.owner_accepted",
                invitee.getScoreboardName(),
                currentClaim.get().getName()
        ));
        return InvitationResult.ACCEPTED;
    }

    public synchronized InvitationResult deny(
            ServerPlayer invitee,
            Claim claim
    ) {
        if (invitee == null || claim == null) {
            return InvitationResult.INVALID_PLAYER;
        }

        Optional<ClaimInvitation> optionalInvitation = findPending(
                claim.getClaimId(),
                invitee.getUUID()
        );
        if (optionalInvitation.isEmpty()) {
            return InvitationResult.INVITATION_NOT_FOUND;
        }

        ClaimInvitation invitation = optionalInvitation.get();
        invitations.remove(invitation.invitationId());
        notifyInviter(invitation, BananaClaimsMessages.text(
                "command.bananaclaims.invite.owner_denied",
                invitee.getScoreboardName(),
                invitation.claimName()
        ));
        return InvitationResult.DENIED;
    }

    public synchronized InvitationResult cancel(
            ServerPlayer actor,
            Claim claim,
            UUID inviteeUuid
    ) {
        if (actor == null || claim == null || inviteeUuid == null) {
            return InvitationResult.INVALID_PLAYER;
        }
        if (!claim.canEditMembers(actor.getUUID())) {
            return InvitationResult.NOT_AUTHORIZED;
        }

        Optional<ClaimInvitation> optionalInvitation = findPending(claim.getClaimId(), inviteeUuid);
        if (optionalInvitation.isEmpty()) {
            return InvitationResult.INVITATION_NOT_FOUND;
        }

        ClaimInvitation invitation = optionalInvitation.get();
        invitations.remove(invitation.invitationId());

        ServerPlayer invitee = actor.level().getServer().getPlayerList().getPlayer(inviteeUuid);
        if (invitee != null) {
            invitee.sendSystemMessage(BananaClaimsMessages.text(
                    "command.bananaclaims.invite.cancelled_notice",
                    claim.getName()
            ));
        }
        return InvitationResult.CANCELLED;
    }

    public synchronized List<ClaimInvitation> getIncoming(UUID playerUuid) {
        pruneExpired(System.currentTimeMillis(), false);
        return invitations.values().stream()
                .filter(invitation -> invitation.inviteeUuid().equals(playerUuid))
                .sorted(Comparator.comparingLong(ClaimInvitation::expiresAtMillis))
                .toList();
    }

    public synchronized List<ClaimInvitation> getOutgoing(UUID playerUuid) {
        pruneExpired(System.currentTimeMillis(), false);
        return invitations.values().stream()
                .filter(invitation -> invitation.inviterUuid().equals(playerUuid))
                .sorted(Comparator.comparingLong(ClaimInvitation::expiresAtMillis))
                .toList();
    }

    public synchronized List<ClaimInvitation> getForClaim(UUID claimId) {
        pruneExpired(System.currentTimeMillis(), false);
        return invitations.values().stream()
                .filter(invitation -> invitation.claimId().equals(claimId))
                .sorted(Comparator.comparingLong(ClaimInvitation::expiresAtMillis))
                .toList();
    }

    public synchronized Optional<ClaimInvitation> getById(UUID invitationId) {
        pruneExpired(System.currentTimeMillis(), false);
        return Optional.ofNullable(invitations.get(invitationId));
    }

    public synchronized Optional<ClaimInvitation> findIncoming(
            UUID playerUuid,
            String selector
    ) {
        pruneExpired(System.currentTimeMillis(), false);
        List<ClaimInvitation> matches = invitations.values().stream()
                .filter(invitation -> invitation.inviteeUuid().equals(playerUuid))
                .filter(invitation -> invitation.matchesSelector(selector))
                .toList();
        return matches.size() == 1 ? Optional.of(matches.getFirst()) : Optional.empty();
    }

    public synchronized int size() {
        pruneExpired(System.currentTimeMillis(), false);
        return invitations.size();
    }

    private Optional<ClaimInvitation> findPending(UUID claimId, UUID inviteeUuid) {
        return invitations.values().stream()
                .filter(invitation -> invitation.claimId().equals(claimId))
                .filter(invitation -> invitation.inviteeUuid().equals(inviteeUuid))
                .findFirst();
    }

    private void tick(MinecraftServer currentServer) {
        server = currentServer;
        long currentTick = currentServer.getTickCount();
        if (currentTick < nextExpiryCheckTick) {
            return;
        }
        nextExpiryCheckTick = currentTick + 20L;
        synchronized (this) {
            pruneExpired(System.currentTimeMillis(), true);
        }
    }

    private void pruneExpired(long now, boolean notify) {
        Iterator<Map.Entry<UUID, ClaimInvitation>> iterator = invitations.entrySet().iterator();
        List<ClaimInvitation> expired = new ArrayList<>();

        while (iterator.hasNext()) {
            ClaimInvitation invitation = iterator.next().getValue();
            if (invitation.isExpired(now)) {
                expired.add(invitation);
                iterator.remove();
            }
        }

        if (!notify || !configManager.shouldNotifyInvitationExpiration() || server == null) {
            return;
        }

        for (ClaimInvitation invitation : expired) {
            ServerPlayer invitee = server.getPlayerList().getPlayer(invitation.inviteeUuid());
            if (invitee != null) {
                invitee.sendSystemMessage(BananaClaimsMessages.text(
                        "command.bananaclaims.invite.expired",
                        invitation.claimName()
                ));
            }
        }
    }

    private synchronized void onClaimChanged(ClaimChangeEvent event) {
        if (event == null) {
            return;
        }

        if (event.getType() == ClaimChangeType.LOADED) {
            invitations.clear();
            return;
        }

        Claim claim = event.getClaim();
        if (claim == null) {
            return;
        }

        if (event.getType() == ClaimChangeType.DELETED
                || event.getType() == ClaimChangeType.OWNERSHIP_TRANSFERRED) {
            invitations.values().removeIf(invitation ->
                    invitation.claimId().equals(claim.getClaimId())
            );
        }
    }

    private void removeForClaimAndInvitee(UUID claimId, UUID inviteeUuid) {
        invitations.values().removeIf(invitation ->
                invitation.claimId().equals(claimId)
                        && invitation.inviteeUuid().equals(inviteeUuid)
        );
    }

    private void notifyInvitee(ServerPlayer invitee, ClaimInvitation invitation) {
        Component message = BananaClaimsMessages.text(
                "command.bananaclaims.invite.received",
                invitation.inviterName(),
                invitation.claimName(),
                configManager.getInvitationExpirationSeconds()
        );

        Component accept = BookComponents.action(
                " [Accept]",
                "/claim invite accept " + invitation.selector(),
                ChatFormatting.GREEN
        );
        Component deny = BookComponents.action(
                " [Deny]",
                "/claim invite deny " + invitation.selector(),
                ChatFormatting.RED
        );

        invitee.sendSystemMessage(message.copy().append(accept).append(deny));
    }

    private void notifyInviter(ClaimInvitation invitation, Component message) {
        MinecraftServer currentServer = server;
        if (currentServer == null) {
            return;
        }
        ServerPlayer inviter = currentServer.getPlayerList().getPlayer(invitation.inviterUuid());
        if (inviter != null) {
            inviter.sendSystemMessage(message);
        }
    }
}
