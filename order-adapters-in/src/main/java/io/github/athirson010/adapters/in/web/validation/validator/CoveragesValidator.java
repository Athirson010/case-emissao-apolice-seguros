package io.github.athirson010.adapters.in.web.validation.validator;

import io.github.athirson010.adapters.in.web.validation.annotation.ValidCoverages;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;
import java.util.Map;

public class CoveragesValidator implements ConstraintValidator<ValidCoverages, Map<String, String>> {

    @Override
    public void initialize(ValidCoverages constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(Map<String, String> coverages, ConstraintValidatorContext context) {
        if (coverages == null || coverages.isEmpty()) {
            return false;
        }

        for (Map.Entry<String, String> entry : coverages.entrySet()) {
            String coverageName = entry.getKey();
            String coverageValue = entry.getValue();

            // Valida que o nome da cobertura não está vazio
            if (coverageName == null || coverageName.isBlank()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Nome da cobertura não pode ser vazio")
                        .addConstraintViolation();
                return false;
            }

            // Valida que o valor da cobertura é numérico e positivo
            if (coverageValue == null || coverageValue.isBlank()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Valor da cobertura '" + coverageName + "' não pode ser vazio")
                        .addConstraintViolation();
                return false;
            }

            try {
                BigDecimal amount = new BigDecimal(coverageValue);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("Valor da cobertura '" + coverageName + "' deve ser maior que zero")
                            .addConstraintViolation();
                    return false;
                }
            } catch (NumberFormatException e) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Valor da cobertura '" + coverageName + "' não é um número válido")
                        .addConstraintViolation();
                return false;
            }
        }

        return true;
    }
}
