package io.github.athirson010.domain.enums;

public enum RiskClassification {
    REGULAR,
    HIGH_RISK,
    PREFERENTIAL,
    NO_INFORMATION;

    public static RiskClassification fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("RiskClassification cannot be null");
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid risk classification: " + value);
        }
    }
}
