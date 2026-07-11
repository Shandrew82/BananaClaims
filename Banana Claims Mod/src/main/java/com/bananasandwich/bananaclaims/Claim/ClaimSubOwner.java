package com.bananasandwich.bananaclaims.claim;

import java.util.Objects;
import java.util.UUID;

public class ClaimSubOwner {

    private UUID uuid;
    private String name;

    public ClaimSubOwner() {
    }

    public ClaimSubOwner(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof ClaimSubOwner other)) {
            return false;
        }

        return Objects.equals(uuid, other.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
