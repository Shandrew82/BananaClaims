package com.bananasandwich.bananaclaims.claim;

import com.bananasandwich.bananaclaims.storage.ClaimStorage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.*;

public final class ClaimManager {
    private static final Map<String, Claim> claimsById = new LinkedHashMap<>();
    private static final Map<String, String> claimIdByChunkKey = new HashMap<>();

    private ClaimManager() {}

    public static void load() {
        claimsById.clear();
        claimIdByChunkKey.clear();
        for (Claim claim : ClaimStorage.loadClaims()) {
            addLoadedClaim(claim);
        }
    }

    public static void save() {
        ClaimStorage.saveClaims(new ArrayList<>(claimsById.values()));
    }

    private static void addLoadedClaim(Claim claim) {
        claimsById.put(claim.id, claim);
        for (ClaimChunk chunk : claim.chunks) {
            claimIdByChunkKey.put(chunk.key(), claim.id);
        }
    }

    public static Claim createClaim(ServerPlayer player, String name) {
        ClaimChunk chunk = currentChunk(player);
        if (getClaimAt(chunk).isPresent()) {
            return null;
        }

        String id = UUID.randomUUID().toString();
        Claim claim = new Claim(
                id,
                name,
                player.getUUID().toString(),
                player.getName().getString(),
                chunk.dimension(),
                chunk
        );

        claimsById.put(id, claim);
        claimIdByChunkKey.put(chunk.key(), id);
        save();
        return claim;
    }

    public static boolean deleteClaimAt(ServerPlayer player) {
        Optional<Claim> claim = getClaimAt(player);
        if (claim.isEmpty()) {
            return false;
        }
        Claim existing = claim.get();
        claimsById.remove(existing.id);
        for (ClaimChunk chunk : existing.chunks) {
            claimIdByChunkKey.remove(chunk.key());
        }
        save();
        return true;
    }

    public static Optional<Claim> getClaimAt(ServerPlayer player) {
        return getClaimAt(currentChunk(player));
    }

    public static Optional<Claim> getClaimAt(ClaimChunk chunk) {
        String id = claimIdByChunkKey.get(chunk.key());
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(claimsById.get(id));
    }

    public static List<Claim> getClaimsFor(ServerPlayer player) {
        String uuid = player.getUUID().toString();
        return claimsById.values().stream()
                .filter(claim -> uuid.equals(claim.ownerUuid))
                .toList();
    }

    public static Optional<Claim> getClaimOwnedByName(ServerPlayer player, String name) {
        String uuid = player.getUUID().toString();
        return claimsById.values().stream()
                .filter(claim -> uuid.equals(claim.ownerUuid))
                .filter(claim -> claim.name.equalsIgnoreCase(name))
                .findFirst();
    }

    public static boolean renameClaim(ServerPlayer player, String oldName, String newName) {
        Optional<Claim> claim = getClaimOwnedByName(player, oldName);
        if (claim.isEmpty()) return false;
        claim.get().name = newName;
        save();
        return true;
    }

    public static boolean setDescription(ServerPlayer player, String name, String description) {
        Optional<Claim> claim = getClaimOwnedByName(player, name);
        if (claim.isEmpty()) return false;
        claim.get().description = description;
        save();
        return true;
    }

    public static Collection<Claim> allClaims() {
        return Collections.unmodifiableCollection(claimsById.values());
    }

    public static ClaimChunk currentChunk(ServerPlayer player) {
        ChunkPos pos = player.chunkPosition();
        String dimension = player.level().dimension().toString();
        return new ClaimChunk(dimension, pos.x(), pos.z());
    }
}
