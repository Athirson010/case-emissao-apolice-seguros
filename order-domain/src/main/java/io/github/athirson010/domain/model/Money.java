package io.github.athirson010.domain.model;

import java.math.BigDecimal;

public record Money(BigDecimal amount, String currency) {

    public Money {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }

    public static Money brl(BigDecimal amount) {
        return new Money(amount, "BRL");
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }
}
