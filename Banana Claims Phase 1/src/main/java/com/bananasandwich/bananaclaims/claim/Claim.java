package com.bananasandwich.bananaclaims.claim;

import java.util.HashSet;
import java.util.Set;

public class Claim {
    public String id;
    public String name;
    public String description = "";
    public String ownerUuid;
    public String ownerName;
    public String dimension;
    public Set<ClaimChunk> chunks = new HashSet<>();

    public Claim() {}

    public Claim(String id, String name, String ownerUuid, String ownerName, String dimension, ClaimChunk chunk) {
        this.id = id;
        this.name = name;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.dimension = dimension;
        this.chunks.add(chunk);
    }

    public boolean contains(ClaimChunk chunk) {
        return chunks.contains(chunk);
    }
}
