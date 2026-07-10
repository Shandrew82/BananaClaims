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

                if (claim.ensureClaimId()) {
                    migrated = true;
                }

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

    public boolean transferOwnership(
            Claim claim,
            UUID newOwnerUuid,
            String newOwnerName
    ) {
        if (claim == null
                || newOwnerUuid == null
                || !claimIdLookup.containsKey(claim.getClaimId())) {
            return false;
        }

        boolean transferred = claim.transferOwnership(
                newOwnerUuid,
                newOwnerName
        );

        if (!transferred) {
            return false;
        }

        revision++;
        save();

        publishChange(
                ClaimChangeType.OWNERSHIP_TRANSFERRED,
                claim
        );

        return true;
    }

    public void saveClaims() {
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

    private void rebuildLookups() {
        chunkLookup.clear();
        claimIdLookup.clear();

        for (Claim claim : claims) {
            claim.ensureClaimId();
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
