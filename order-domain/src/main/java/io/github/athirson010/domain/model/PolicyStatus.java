package io.github.athirson010.domain.model;

public enum PolicyStatus {
    RECEIVED,
    VALIDATED,
    PENDING,
    APPROVED,
    REJECTED,
    CANCELED;

    public boolean isFinalState() {
        return this == APPROVED || this == REJECTED || this == CANCELED;
    }
}
