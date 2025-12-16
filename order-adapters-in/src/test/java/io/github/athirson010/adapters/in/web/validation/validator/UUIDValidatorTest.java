package io.github.athirson010.adapters.in.web.validation.validator;

import io.github.athirson010.adapters.in.web.validation.annotation.ValidUUID;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("UUIDValidator - Testes Unitários")
class UUIDValidatorTest {

    @Mock
    private ValidUUID annotation;

    @Mock
    private ConstraintValidatorContext context;

    private UUIDValidator validator;

    @BeforeEach
    void setUp() {
        validator = new UUIDValidator();
        validator.initialize(annotation);
    }

    @Test
    @DisplayName("Deve validar UUID válido com sucesso")
    void deveValidarUuidValidoComSucesso() {
        // Given
        String validUuid = "123e4567-e89b-12d3-a456-426614174000";

        // When
        boolean result = validator.isValid(validUuid, context);

        // Then
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "550e8400-e29b-41d4-a716-446655440000",
            "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
            "00000000-0000-0000-0000-000000000000",
            "ffffffff-ffff-ffff-ffff-ffffffffffff"
    })
    @DisplayName("Deve validar múltiplos UUIDs válidos")
    void deveValidarMultiplosUuidsValidos(String uuid) {
        // When
        boolean result = validator.isValid(uuid, context);

        // Then
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Deve rejeitar UUID nulo, vazio ou em branco")
    void deveRejeitarUuidNuloVazioOuEmBranco(String uuid) {
        // When
        boolean result = validator.isValid(uuid, context);

        // Then
        assertThat(result).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "invalid-uuid",
            "123",
            "123e4567-e89b-12d3-a456",
            "123e4567-e89b-12d3-a456-42661417400g",
            "123e4567e89b12d3a456426614174000",
            "123e4567-e89b-12d3-a456-426614174000-extra",
            "not-a-uuid-at-all"
    })
    @DisplayName("Deve rejeitar UUID com formato inválido")
    void deveRejeitarUuidComFormatoInvalido(String uuid) {
        // When
        boolean result = validator.isValid(uuid, context);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar UUID com letras maiúsculas inválidas")
    void deveRejeitarUuidComLetrasMaiusculasInvalidas() {
        // Given
        String invalidUuid = "123G4567-e89b-12d3-a456-426614174000";

        // When
        boolean result = validator.isValid(invalidUuid, context);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Deve aceitar UUID em letras maiúsculas")
    void deveAceitarUuidEmLetrasMaiusculas() {
        // Given
        String upperCaseUuid = "123E4567-E89B-12D3-A456-426614174000";

        // When
        boolean result = validator.isValid(upperCaseUuid, context);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Deve aceitar UUID em letras minúsculas")
    void deveAceitarUuidEmLetrasMinusculas() {
        // Given
        String lowerCaseUuid = "123e4567-e89b-12d3-a456-426614174000";

        // When
        boolean result = validator.isValid(lowerCaseUuid, context);

        // Then
        assertThat(result).isTrue();
    }
}
