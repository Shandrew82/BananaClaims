package com.bananasandwich.bananaclaims.claim;

/**
 * Domain result returned by lifecycle mutations. Commands translate these
 * results into player-facing text while ClaimManager remains responsible for
 * authorization, invariants, persistence, and change events.
 */
public enum ClaimMutationResult {
    MEMBER_ADDED(true),
    MEMBER_REMOVED(true),
    MEMBER_LEFT(true),
    SUBOWNER_ADDED(true),
    MEMBER_PROMOTED_TO_SUBOWNER(true),
    SUBOWNER_DEMOTED_TO_MEMBER(true),
    SUBOWNER_STEPPED_DOWN(true),
    OWNERSHIP_TRANSFERRED(true),

    CLAIM_NOT_FOUND(false),
    INVALID_PLAYER(false),
    NOT_AUTHORIZED(false),
    PLAYER_IS_OWNER(false),
    PLAYER_IS_SUBOWNER(false),
    PLAYER_IS_MEMBER(false),
    PLAYER_NOT_MEMBER(false),
    PLAYER_NOT_SUBOWNER(false),
    OWNER_CANNOT_LEAVE(false),
    NOT_PARTICIPANT(false),
    SAME_OWNER(false),
    DUPLICATE_OWNER_CLAIM_NAME(false),
    NO_CHANGE(false);

    private final boolean success;

    ClaimMutationResult(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }
}
