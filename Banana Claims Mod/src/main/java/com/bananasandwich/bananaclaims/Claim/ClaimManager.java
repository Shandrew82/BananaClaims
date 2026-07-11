package com.bananasandwich.bananaclaims.claim;

import com.bananasandwich.bananaclaims.claim.event.ClaimChangeEvent;
import com.bananasandwich.bananaclaims.claim.event.ClaimChangeListener;
import com.bananasandwich.bananaclaims.claim.event.ClaimChangeType;
import com.bananasandwich.bananaclaims.claim.event.ClaimEventBus;
import com.bananasandwich.bananaclaims.storage.ClaimStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ClaimManager {

    private final List<Claim> claims = new ArrayList<>();
    private final Map<ClaimChunkKey, Claim> chunkLookup = new HashMap<>();
    private final Map<UUID, Claim> claimIdLookup = new HashMap<>();
    private final ClaimEventBus eventBus = new ClaimEventBus();

    private ClaimStorage storage;
    private long revision = 0;

    public void setStorage(ClaimStorage storage) {
        this.storage = storage;
    }

    public void addChangeListener(ClaimChangeListener listener) {
        eventBus.register(listener);
    }

    public void removeChangeListener(ClaimChangeListener listener) {
        eventBus.unregister(listener);
    }

    public boolean addClaim(Claim claim) {
        if (claim == null) {
            return false;
        }

        claim.ensureClaimId();
        claim.repairRoleInvariants();

        if (claimIdLookup.containsKey(claim.getClaimId())) {
            return false;
        }

        for (ClaimChunk chunk : claim.getChunks()) {
            if (isChunkClaimed(
                    chunk.getDimension(),
                    chunk.getChunkX(),
                    chunk.getChunkZ()
            )) {
                return false;
            }
        }

        claims.add(claim);
        rebuildLookups();
        save();

        publishChange(
                ClaimChangeType.CREATED,
                claim
        );

        return true;
    }

    public void loadClaims(List<Claim> loadedClaims) {
        claims.clear();

        boolean migrated = false;

        if (loadedClaims != null) {
            for (Claim claim : loadedClaims) {
                if (claim == null) {
                    continue;
                }

                migrated |= claim.ensureClaimId();
                migrated |= claim.repairRoleInvariants();
                claims.add(claim);
            }
        }

        rebuildLookups();

        if (migrated) {
            save();
        }

        publishChange(
                ClaimChangeType.LOADED,
                null
        );
    }

    public boolean removeClaim(
            String dimension,
            int chunkX,
            int chunkZ
    ) {
        Claim claim = chunkLookup.get(
                new ClaimChunkKey(
                        dimension,
                        chunkX,
                        chunkZ
                )
        );

        if (claim == null) {
            return false;
        }

        boolean removed = claims.remove(claim);

        if (removed) {
            rebuildLookups();
            save();

            publishChange(
                    ClaimChangeType.DELETED,
                    claim
            );
        }

        return removed;
    }

    public ClaimMutationResult addMember(
            Claim claim,
            UUID actorUuid,
            UUID playerUuid,
            String playerName
    ) {
        ClaimMutationResult validation = validateManagedMutation(
                claim,
                actorUuid,
                playerUuid,
                true
        );

        if (validation != ClaimMutationResult.NO_CHANGE) {
            return validation;
        }

        return switch (claim.getRole(playerUuid)) {
            case OWNER -> ClaimMutationResult.PLAYER_IS_OWNER;
            case SUBOWNER -> ClaimMutationResult.PLAYER_IS_SUBOWNER;
            case MEMBER -> ClaimMutationResult.PLAYER_IS_MEMBER;
            case NONE -> {
                if (!claim.addMember(playerUuid, playerName)) {
                    yield ClaimMutationResult.NO_CHANGE;
                }

                commitMutation(
                        ClaimChangeType.MEMBERSHIP_CHANGED,
                        claim
                );

                yield ClaimMutationResult.MEMBER_ADDED;
            }
        };
    }

    public ClaimMutationResult removeMember(
            Claim claim,
            UUID actorUuid,
            UUID playerUuid
    ) {
        ClaimMutationResult validation = validateManagedMutation(
                claim,
                actorUuid,
                playerUuid,
                true
        );

        if (validation != ClaimMutationResult.NO_CHANGE) {
            return validation;
        }

        return switch (claim.getRole(playerUuid)) {
            case OWNER -> ClaimMutationResult.PLAYER_IS_OWNER;
            case SUBOWNER -> ClaimMutationResult.PLAYER_IS_SUBOWNER;
            case NONE -> ClaimMutationResult.PLAYER_NOT_MEMBER;
            case MEMBER -> {
                if (!claim.removeMember(playerUuid)) {
                    yield ClaimMutationResult.NO_CHANGE;
                }

                commitMutation(
                        ClaimChangeType.MEMBERSHIP_CHANGED,
                        claim
                );

                yield ClaimMutationResult.MEMBER_REMOVED;
            }
        };
    }

    public ClaimMutationResult addSubOwner(
            Claim claim,
            UUID actorUuid,
            UUID playerUuid,
            String playerName
    ) {
        ClaimMutationResult validation = validateManagedMutation(
                claim,
                actorUuid,
                playerUuid,
                false
        );

        if (validation != ClaimMutationResult.NO_CHANGE) {
            return validation;
        }

        if (!claim.canEditSubOwners(actorUuid)) {
            return ClaimMutationResult.NOT_AUTHORIZED;
        }

        ClaimRole previousRole = claim.getRole(playerUuid);

        if (previousRole == ClaimRole.OWNER) {
            return ClaimMutationResult.PLAYER_IS_OWNER;
        }

        if (previousRole == ClaimRole.SUBOWNER) {
            return ClaimMutationResult.PLAYER_IS_SUBOWNER;
        }

        if (!claim.addSubOwner(playerUuid, playerName)) {
            return ClaimMutationResult.NO_CHANGE;
        }

        commitMutation(
                ClaimChangeType.SUBOWNER_CHANGED,
                claim
        );

        return previousRole == ClaimRole.MEMBER
                ? ClaimMutationResult.MEMBER_PROMOTED_TO_SUBOWNER
                : ClaimMutationResult.SUBOWNER_ADDED;
    }

    public ClaimMutationResult demoteSubOwner(
            Claim claim,
            UUID actorUuid,
            UUID playerUuid
    ) {
        ClaimMutationResult validation = validateManagedMutation(
                claim,
                actorUuid,
                playerUuid,
                false
        );

        if (validation != ClaimMutationResult.NO_CHANGE) {
            return validation;
        }

        if (!claim.canEditSubOwners(actorUuid)) {
            return ClaimMutationResult.NOT_AUTHORIZED;
        }

        ClaimRole targetRole = claim.getRole(playerUuid);

        if (targetRole == ClaimRole.OWNER) {
            return ClaimMutationResult.PLAYER_IS_OWNER;
        }

        if (targetRole != ClaimRole.SUBOWNER) {
            return ClaimMutationResult.PLAYER_NOT_SUBOWNER;
        }

        if (!claim.demoteSubOwnerToMember(playerUuid)) {
            return ClaimMutationResult.NO_CHANGE;
        }

        commitMutation(
                ClaimChangeType.SUBOWNER_CHANGED,
                claim
        );

        return ClaimMutationResult.SUBOWNER_DEMOTED_TO_MEMBER;
    }

    public ClaimMutationResult leaveClaim(
            Claim claim,
            UUID playerUuid
    ) {
        if (!isManagedClaim(claim)) {
            return ClaimMutationResult.CLAIM_NOT_FOUND;
        }

        if (playerUuid == null) {
            return ClaimMutationResult.INVALID_PLAYER;
        }

        return switch (claim.getRole(playerUuid)) {
            case OWNER -> ClaimMutationResult.OWNER_CANNOT_LEAVE;
            case NONE -> ClaimMutationResult.NOT_PARTICIPANT;
            case MEMBER -> {
                if (!claim.removeMember(playerUuid)) {
                    yield ClaimMutationResult.NO_CHANGE;
                }

                commitMutation(
                        ClaimChangeType.MEMBERSHIP_CHANGED,
                        claim
                );

                yield ClaimMutationResult.MEMBER_LEFT;
            }
            case SUBOWNER -> {
                if (!claim.demoteSubOwnerToMember(playerUuid)) {
                    yield ClaimMutationResult.NO_CHANGE;
                }

                commitMutation(
                        ClaimChangeType.SUBOWNER_CHANGED,
                        claim
                );

                yield ClaimMutationResult.SUBOWNER_STEPPED_DOWN;
            }
        };
    }

    public ClaimMutationResult transferOwnership(
            Claim claim,
            UUID actorUuid,
            UUID newOwnerUuid,
            String newOwnerName
    ) {
        if (!isManagedClaim(claim)) {
            return ClaimMutationResult.CLAIM_NOT_FOUND;
        }

        if (actorUuid == null || newOwnerUuid == null) {
            return ClaimMutationResult.INVALID_PLAYER;
        }

        if (!claim.canTransfer(actorUuid)) {
            return ClaimMutationResult.NOT_AUTHORIZED;
        }

        if (claim.isOwner(newOwnerUuid)) {
            return ClaimMutationResult.SAME_OWNER;
        }

        boolean duplicateClaimName = claims.stream()
                .filter(ownedClaim ->
                        ownedClaim != claim
                                && ownedClaim.isOwner(newOwnerUuid)
                )
                .map(Claim::getName)
                .filter(ownedName ->
                        ownedName != null
                                && claim.getName() != null
                )
                .anyMatch(ownedName ->
                        ownedName.equalsIgnoreCase(claim.getName())
                );

        if (duplicateClaimName) {
            return ClaimMutationResult.DUPLICATE_OWNER_CLAIM_NAME;
        }

        if (!claim.transferOwnership(
                newOwnerUuid,
                newOwnerName
        )) {
            return ClaimMutationResult.NO_CHANGE;
        }

        commitMutation(
                ClaimChangeType.OWNERSHIP_TRANSFERRED,
                claim
        );

        return ClaimMutationResult.OWNERSHIP_TRANSFERRED;
    }

    public void saveClaims() {
        for (Claim claim : claims) {
            claim.repairRoleInvariants();
        }

        rebuildLookups();
        save();

        publishChange(
                ClaimChangeType.UPDATED,
                null
        );
    }

    public Optional<Claim> getClaimById(UUID claimId) {
        if (claimId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                claimIdLookup.get(claimId)
        );
    }

    public Optional<Claim> getClaimAt(
            String dimension,
            int chunkX,
            int chunkZ
    ) {
        return Optional.ofNullable(
                chunkLookup.get(
                        new ClaimChunkKey(
                                dimension,
                                chunkX,
                                chunkZ
                        )
                )
        );
    }

    public List<Claim> getClaimsForOwner(UUID ownerUuid) {
        return claims.stream()
                .filter(claim ->
                        claim.getOwnerUuid() != null
                                && claim.getOwnerUuid().equals(ownerUuid)
                )
                .toList();
    }

    public List<Claim> getAllClaims() {
        return List.copyOf(claims);
    }

    public boolean isChunkClaimed(
            String dimension,
            int chunkX,
            int chunkZ
    ) {
        return chunkLookup.containsKey(
                new ClaimChunkKey(
                        dimension,
                        chunkX,
                        chunkZ
                )
        );
    }

    public long getRevision() {
        return revision;
    }

    private ClaimMutationResult validateManagedMutation(
            Claim claim,
            UUID actorUuid,
            UUID playerUuid,
            boolean memberPermission
    ) {
        if (!isManagedClaim(claim)) {
            return ClaimMutationResult.CLAIM_NOT_FOUND;
        }

        if (actorUuid == null || playerUuid == null) {
            return ClaimMutationResult.INVALID_PLAYER;
        }

        if (memberPermission
                && !claim.canEditMembers(actorUuid)) {
            return ClaimMutationResult.NOT_AUTHORIZED;
        }

        return ClaimMutationResult.NO_CHANGE;
    }

    private boolean isManagedClaim(Claim claim) {
        if (claim == null) {
            return false;
        }

        UUID claimId = claim.getClaimId();
        return claimId != null
                && claimIdLookup.get(claimId) == claim;
    }

    private void commitMutation(
            ClaimChangeType type,
            Claim claim
    ) {
        claim.repairRoleInvariants();
        revision++;
        save();
        publishChange(type, claim);
    }

    private void rebuildLookups() {
        chunkLookup.clear();
        claimIdLookup.clear();

        for (Claim claim : claims) {
            claim.ensureClaimId();
            claim.repairRoleInvariants();

            claimIdLookup.put(
                    claim.getClaimId(),
                    claim
            );

            for (ClaimChunk chunk : claim.getChunks()) {
                ClaimChunkKey key = new ClaimChunkKey(
                        chunk.getDimension(),
                        chunk.getChunkX(),
                        chunk.getChunkZ()
                );

                chunkLookup.put(
                        key,
                        claim
                );
            }
        }

        revision++;
    }

    private void publishChange(
            ClaimChangeType type,
            Claim claim
    ) {
        eventBus.publish(
                new ClaimChangeEvent(
                        type,
                        claim,
                        revision
                )
        );
    }

    private void save() {
        if (storage != null) {
            storage.saveClaims(getAllClaims());
        }
    }
}
