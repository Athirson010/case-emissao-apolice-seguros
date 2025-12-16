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
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
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
        @ValueSource(strings = {
                "0.01",
                "1.00",
                "100.00",
                "1000.50",
                "999999.99",
                "350.00",
                "200000.00"
        })
        @DisplayName("Deve validar valores positivos válidos")
        void deveValidarValoresPositivosValidos(String value) {
            // When
            boolean result = validator.isValid(value, context);

            // Then
            assertThat(result).isTrue();
            verify(context, never()).disableDefaultConstraintViolation();
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "0.00", "0.0"})
        @DisplayName("Deve rejeitar valor zero quando positive=true")
        void deveRejeitarValorZeroQuandoPositiveTrue(String value) {
            // When
            boolean result = validator.isValid(value, context);

            // Then
            assertThat(result).isFalse();
            verify(context).disableDefaultConstraintViolation();
            verify(context).buildConstraintViolationWithTemplate("Valor deve ser maior que zero");
        }

        @ParameterizedTest
        @ValueSource(strings = {"-0.01", "-1.00", "-100.00", "-999999.99"})
        @DisplayName("Deve rejeitar valores negativos quando positive=true")
        void deveRejeitarValoresNegativosQuandoPositiveTrue(String value) {
            // When
            boolean result = validator.isValid(value, context);

            // Then
            assertThat(result).isFalse();
            verify(context).disableDefaultConstraintViolation();
            verify(context).buildConstraintViolationWithTemplate("Valor deve ser maior que zero");
        }
    }

    @Nested
    @DisplayName("Validação com positive=false (valores não-negativos permitidos)")
    class NonNegativeValidation {

        @BeforeEach
        void setUp() {
            when(annotation.positive()).thenReturn(false);
            validator.initialize(annotation);
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "0.00", "0.01", "1.00", "100.00"})
        @DisplayName("Deve validar valores zero e positivos quando positive=false")
        void deveValidarValoresZeroEPositivosQuandoPositiveFalse(String value) {
            // When
            boolean result = validator.isValid(value, context);

            // Then
            assertThat(result).isTrue();
            verify(context, never()).disableDefaultConstraintViolation();
        }

        @ParameterizedTest
        @ValueSource(strings = {"-0.01", "-1.00", "-100.00"})
        @DisplayName("Deve validar valores negativos quando positive=false")
        void deveValidarValoresNegativosQuandoPositiveFalse(String value) {
            // When
            boolean result = validator.isValid(value, context);

            // Then
            assertThat(result).isTrue();
            verify(context, never()).disableDefaultConstraintViolation();
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
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Deve rejeitar valores nulos, vazios ou em branco")
        void deveRejeitarValoresNulosVaziosOuEmBranco(String value) {
            // When
            boolean result = validator.isValid(value, context);

            // Then
            assertThat(result).isFalse();
            verify(context, never()).disableDefaultConstraintViolation();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "abc",
                "12.34.56",
                "R$ 100,00",
                "100,00",
                "1.000.000",
                "invalid",
                "12a34",
                "12.34a"
        })
        @DisplayName("Deve rejeitar formato numérico inválido")
        void deveRejeitarFormatoNumericoInvalido(String value) {
            // When
            boolean result = validator.isValid(value, context);

            // Then
            assertThat(result).isFalse();
            verify(context).disableDefaultConstraintViolation();
            verify(context).buildConstraintViolationWithTemplate("Formato numérico inválido");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "123",
                "123.45",
                "0.01",
                "999999.99",
                "1000000",
                "12345678901234567890.12345678901234567890"
        })
        @DisplayName("Deve validar diferentes formatos numéricos válidos")
        void deveValidarDiferentesFormatosNumericosValidos(String value) {
            // When
            boolean result = validator.isValid(value, context);

            // Then
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("Casos especiais")
    class SpecialCases {

        @BeforeEach
        void setUp() {
            when(annotation.positive()).thenReturn(true);
            validator.initialize(annotation);
        }

        @Test
        @DisplayName("Deve validar número muito grande")
        void deveValidarNumeroMuitoGrande() {
            // Given
            String value = "99999999999999999999.99";

            // When
            boolean result = validator.isValid(value, context);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Deve validar número muito pequeno positivo")
        void deveValidarNumeroMuitoPequenoPositivo() {
            // Given
            String value = "0.00000001";

            // When
            boolean result = validator.isValid(value, context);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Deve validar número sem casas decimais")
        void deveValidarNumeroSemCasasDecimais() {
            // Given
            String value = "1000";

            // When
            boolean result = validator.isValid(value, context);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Deve validar número com muitas casas decimais")
        void deveValidarNumeroComMuitasCasasDecimais() {
            // Given
            String value = "123.123456789012345";

            // When
            boolean result = validator.isValid(value, context);

            // Then
            assertThat(result).isTrue();
        }
    }
}
