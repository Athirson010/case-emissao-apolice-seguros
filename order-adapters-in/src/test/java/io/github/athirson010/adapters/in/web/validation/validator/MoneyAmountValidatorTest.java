package io.github.athirson010.adapters.in.web.validation.validator;

import io.github.athirson010.adapters.in.web.validation.annotation.ValidMoneyAmount;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MoneyAmountValidator - Testes Unitários")
class MoneyAmountValidatorTest {

    @Mock
    private ValidMoneyAmount annotation;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    private MoneyAmountValidator validator;

    @BeforeEach
    void setUp() {
        validator = new MoneyAmountValidator();

        lenient()
                .when(context.buildConstraintViolationWithTemplate(anyString()))
                .thenReturn(violationBuilder);

        lenient()
                .when(violationBuilder.addConstraintViolation())
                .thenReturn(context);
    }

    @Nested
    @DisplayName("Validação com positive=true (valores positivos obrigatórios)")
    class PositiveValidation {

        @BeforeEach
        void setUp() {
            when(annotation.positive()).thenReturn(true);
            validator.initialize(annotation);
        }

        @ParameterizedTest
        @ValueSource(strings = {"0.01", "1.00", "100.00"})
        void deveValidarValoresPositivosValidos(String value) {
            assertThat(validator.isValid(value, context)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "0.00", "-1.00"})
        void deveRejeitarValoresInvalidos(String value) {
            assertThat(validator.isValid(value, context)).isFalse();
        }
    }

    @Nested
    @DisplayName("Validação de formato numérico")
    class NumericFormatValidation {

        @BeforeEach
        void setUp() {
            when(annotation.positive()).thenReturn(true);
            validator.initialize(annotation);
        }

        @ParameterizedTest
        @NullAndEmptySource
        void deveRejeitarValoresNulosOuVazios(String value) {
            assertThat(validator.isValid(value, context)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"abc", "100,00", "R$ 10"})
        void deveRejeitarFormatoInvalido(String value) {
            assertThat(validator.isValid(value, context)).isFalse();
        }
    }
}
