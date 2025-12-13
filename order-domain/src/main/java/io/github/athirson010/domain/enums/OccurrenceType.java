package io.github.athirson010.domain.enums;

public enum OccurrenceType {
    FRAUD,
    SUSPICION;

    public static OccurrenceType fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("OccurrenceType cannot be null");
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid occurrence type: " + value);
        }
    }
}
