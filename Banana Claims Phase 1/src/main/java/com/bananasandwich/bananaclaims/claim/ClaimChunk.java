package com.bananasandwich.bananaclaims.claim;

public record ClaimChunk(String dimension, int x, int z) {
    public String key() {
        return dimension + ":" + x + ":" + z;
    }
}
