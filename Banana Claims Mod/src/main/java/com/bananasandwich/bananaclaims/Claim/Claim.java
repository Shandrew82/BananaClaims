
package com.bananasandwich.bananaclaims.claim;

import java.util.UUID;

public class Claim {
    private final String name;
    private final UUID ownerUuid;
    private final String ownerName;
    private final String dimension;
    private final int chunkX;
    private final int chunkZ;
    private String description;

    public Claim(String name, UUID ownerUuid, String ownerName, String dimension, int chunkX, int chunkZ) {
        this.name = name;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.dimension = dimension;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.description = "";
    }

    public String getName() {
        return name;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getDimension() {
        return dimension;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}