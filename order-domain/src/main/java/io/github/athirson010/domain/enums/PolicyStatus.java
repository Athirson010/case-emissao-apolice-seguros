package io.github.athirson010.domain.enums;

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

    public static PolicyStatus fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("PolicyStatus cannot be null");
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid policy status: " + value);
        }
    }
}
