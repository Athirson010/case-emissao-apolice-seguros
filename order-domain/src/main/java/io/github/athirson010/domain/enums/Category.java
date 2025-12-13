package io.github.athirson010.domain.enums;

public enum Category {
    AUTO,
    VIDA,
    RESIDENCIAL,
    EMPRESARIAL,
    OUTROS;

    public static Category fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid category: " + value);
        }
    }
}
