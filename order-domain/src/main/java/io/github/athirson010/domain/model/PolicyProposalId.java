package io.github.athirson010.domain.model;

import java.util.UUID;

public record PolicyProposalId(UUID value) {

    public static PolicyProposalId generate() {
        return new PolicyProposalId(UUID.randomUUID());
    }

    public static PolicyProposalId from(String id) {
        return new PolicyProposalId(UUID.fromString(id));
    }

    public static PolicyProposalId from(UUID id) {
        return new PolicyProposalId(id);
    }

    public String asString() {
        return value.toString();
    }
}
