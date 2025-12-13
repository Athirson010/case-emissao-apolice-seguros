package io.github.athirson010.adapters.in.web.validation.annotation;

import io.github.athirson010.adapters.in.web.validation.validator.UUIDValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UUIDValidator.class)
@Documented
public @interface ValidUUID {
    String message() default "UUID inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
