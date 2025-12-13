package io.github.athirson010.domain.enums;

public enum SalesChannel {
    MOBILE,
    WEB,
    WHATSAPP,
    OUTROS;

    public static SalesChannel fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("SalesChannel cannot be null");
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid sales channel: " + value);
        }
    }
}
