package io.github.athirson010.adapters.in.web.validation.annotation;

import io.github.athirson010.adapters.in.web.validation.validator.MoneyAmountValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MoneyAmountValidator.class)
@Documented
public @interface ValidMoneyAmount {
    String message() default "Valor monetário inválido";

    boolean positive() default true;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
