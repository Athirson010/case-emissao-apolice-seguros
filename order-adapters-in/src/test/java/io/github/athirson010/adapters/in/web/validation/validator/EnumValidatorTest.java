package io.github.athirson010.adapters.in.web.validation.validator;

import io.github.athirson010.adapters.in.web.validation.annotation.ValidEnum;
import io.github.athirson010.domain.enums.Category;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnumValidator - Testes Unitários")
class EnumValidatorTest {

    @Mock
    private ValidEnum annotation;

    @Mock
    private ConstraintValidatorContext context;

    private EnumValidator validator;

    @BeforeEach
    void setUp() {
        validator = new EnumValidator();
        // Configura o mock para retornar Category.class
        when(annotation.enumClass()).thenReturn((Class) Category.class);
        validator.initialize(annotation);
    }

    @ParameterizedTest
    @ValueSource(strings = {"AUTO", "VIDA", "RESIDENCIAL", "EMPRESARIAL", "OUTROS"})
    @DisplayName("Deve validar valores enum válidos em maiúsculas")
    void deveValidarValoresEnumValidosEmMaiusculas(String value) {
        // When
        boolean result = validator.isValid(value, context);

        // Then
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"auto", "vida", "residencial", "empresarial", "outros"})
    @DisplayName("Deve validar valores enum válidos em minúsculas")
    void deveValidarValoresEnumValidosEmMinusculas(String value) {
        // When
        boolean result = validator.isValid(value, context);

        // Then
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Auto", "Vida", "ReSiDeNcIaL", "EmpresariaL", "OuTrOs"})
    @DisplayName("Deve validar valores enum válidos em case misto")
    void deveValidarValoresEnumValidosEmCaseMisto(String value) {
        // When
        boolean result = validator.isValid(value, context);

        // Then
        assertThat(result).isTrue();
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
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "INVALID",
            "SAUDE",
            "TRANSPORTE",
            "AUTO_EXTRA",
            "123",
            "auto vida",
            "AUTO-VIDA"
    })
    @DisplayName("Deve rejeitar valores que não existem no enum")
    void deveRejeitarValoresQueNaoExistemNoEnum(String value) {
        // When
        boolean result = validator.isValid(value, context);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar valor com espaços no início e fim")
    void deveRejeitarValorComEspacosNoInicioEFim() {
        // Given
        String value = " AUTO ";

        // When
        boolean result = validator.isValid(value, context);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Deve validar todos os valores do enum Category")
    void deveValidarTodosOsValoresDoEnumCategory() {
        // When & Then
        for (Category category : Category.values()) {
            boolean result = validator.isValid(category.name(), context);
            assertThat(result)
                    .as("Category %s deve ser válida", category.name())
                    .isTrue();
        }
    }
}
