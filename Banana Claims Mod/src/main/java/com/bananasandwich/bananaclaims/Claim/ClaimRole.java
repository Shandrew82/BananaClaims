package com.bananasandwich.bananaclaims.claim;

/**
 * The mutually exclusive role a player holds inside a claim.
 */
public enum ClaimRole {
    OWNER,
    SUBOWNER,
    MEMBER,
    NONE;

    public boolean hasAccess() {
        return this != NONE;
    }

    public boolean canManage() {
        return this == OWNER || this == SUBOWNER;
    }
}
