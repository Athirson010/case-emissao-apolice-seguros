package io.github.athirson010.adapters.in.web.validation.validator;

import io.github.athirson010.adapters.in.web.validation.annotation.ValidMoneyAmount;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class MoneyAmountValidator implements ConstraintValidator<ValidMoneyAmount, String> {

    private boolean positive;

    @Override
    public void initialize(ValidMoneyAmount constraintAnnotation) {
        this.positive = constraintAnnotation.positive();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }

        try {
            BigDecimal amount = new BigDecimal(value);

            if (positive && amount.compareTo(BigDecimal.ZERO) <= 0) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Valor deve ser maior que zero")
                        .addConstraintViolation();
                return false;
            }

            return true;
        } catch (NumberFormatException e) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Formato numérico inválido")
                    .addConstraintViolation();
            return false;
        }
    }
}
