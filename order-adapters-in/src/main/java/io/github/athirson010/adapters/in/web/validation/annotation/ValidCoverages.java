package io.github.athirson010.adapters.in.web.validation.annotation;

import io.github.athirson010.adapters.in.web.validation.validator.CoveragesValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CoveragesValidator.class)
@Documented
public @interface ValidCoverages {
    String message() default "Coberturas contêm valores inválidos";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
