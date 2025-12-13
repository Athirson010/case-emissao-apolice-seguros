package io.github.athirson010.domain.enums;

public enum PaymentMethod {
    CREDIT_CARD,
    DEBIT,
    BOLETO,
    PIX;

    public static PaymentMethod fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("PaymentMethod cannot be null");
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid payment method: " + value);
        }
    }
}
