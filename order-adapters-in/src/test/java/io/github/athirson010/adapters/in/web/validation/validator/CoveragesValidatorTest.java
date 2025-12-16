package io.github.athirson010.adapters.in.web.validation.validator;

import io.github.athirson010.adapters.in.web.validation.annotation.ValidCoverages;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CoveragesValidator - Testes Unitários")
class CoveragesValidatorTest {

    @Mock
    private ValidCoverages annotation;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    private CoveragesValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CoveragesValidator();
        validator.initialize(annotation);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
    }

    @Test
    @DisplayName("Deve validar coberturas válidas com sucesso")
    void deveValidarCoberturasValidasComSucesso() {
        // Given
        Map<String, String> coverages = new HashMap<>();
        coverages.put("COLISAO", "100000.00");
        coverages.put("ROUBO", "50000.00");
        coverages.put("INCENDIO", "75000.00");

        // When
        boolean result = validator.isValid(coverages, context);

        // Then
        assertThat(result).isTrue();
        verify(context, never()).disableDefaultConstraintViolation();
    }

    @Test
    @DisplayName("Deve validar cobertura única válida")
    void deveValidarCoberturaUnicaValida() {
        // Given
        Map<String, String> coverages = Map.of("COLISAO", "200000.00");

        // When
        boolean result = validator.isValid(coverages, context);

        // Then
        assertThat(result).isTrue();
        verify(context, never()).disableDefaultConstraintViolation();
    }

    @Test
    @DisplayName("Deve rejeitar mapa de coberturas nulo")
    void deveRejeitarMapaDeCoberturas Nulo() {
        // When
        boolean result = validator.isValid(null, context);

        // Then
        assertThat(result).isFalse();
        verify(context, never()).disableDefaultConstraintViolation();
    }

    @Test
    @DisplayName("Deve rejeitar mapa de coberturas vazio")
    void deveRejeitarMapaDeCoberturasVazio() {
        // Given
        Map<String, String> coverages = new HashMap<>();

        // When
        boolean result = validator.isValid(coverages, context);

        // Then
        assertThat(result).isFalse();
        verify(context, never()).disableDefaultConstraintViolation();
    }

    @Test
    @DisplayName("Deve rejeitar cobertura com nome nulo")
    void deveRejeitarCoberturaComNomeNulo() {
        // Given
        Map<String, String> coverages = new HashMap<>();
        coverages.put(null, "100000.00");

        // When
        boolean result = validator.isValid(coverages, context);

        // Then
        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Nome da cobertura não pode ser vazio");
    }

    @Test
    @DisplayName("Deve rejeitar cobertura com nome vazio")
    void deveRejeitarCoberturaComNomeVazio() {
        // Given
        Map<String, String> coverages = Map.of("", "100000.00");

        // When
        boolean result = validator.isValid(coverages, context);

        // Then
        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Nome da cobertura não pode ser vazio");
    }

    @Test
    @DisplayName("Deve rejeitar cobertura com nome em branco")
    void deveRejeitarCoberturaComNomeEmBranco() {
        // Given
        Map<String, String> coverages = Map.of("   ", "100000.00");

        // When
        boolean result = validator.isValid(coverages, context);

        // Then
        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Nome da cobertura não pode ser vazio");
    }

    @Test
    @DisplayName("Deve rejeitar cobertura com valor nulo")
    void deveRejeitarCoberturaComValorNulo() {
        // Given
        Map<String, String> coverages = new HashMap<>();
        coverages.put("COLISAO", null);

        // When
        boolean result = validator.isValid(coverages, context);

        // Then
        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Valor da cobertura 'COLISAO' não pode ser vazio");
    }

    @Test
    @DisplayName("Deve rejeitar cobertura com valor vazio")
    void deveRejeitarCoberturaComValorVazio() {
        // Given
        Map<String, String> coverages = Map.of("COLISAO", "");

        // When
        boolean result = validator.isValid(coverages, context);

        // Then
        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Valor da cobertura 'COLISAO' não pode ser vazio");
    }

    @Test
    @DisplayName("Deve rejeitar cobertura com valor em branco")
    void deveRejeitarCoberturaComValorEmBranco() {
        // Given
        Map<String, String> coverages = Map.of("COLISAO", "   ");

        // When
        boolean result = validator.isValid(coverages, context);

        // Then
        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Valor da cobertura 'COLISAO' não pode ser vazio");
    }

    @Test
    @DisplayName("Deve rejeitar cobertura com valor zero")
    void deveRejeitarCoberturaComValorZero() {
        // Given
        Map<String, String> coverages = Map.of("COLISAO", "0");

        // When
        boolean result = validator.isValid(coverages, context);

        // Then
        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Valor da cobertura 'COLISAO' deve ser maior que zero");
    }

    @Test
    @DisplayName("Deve rejeitar cobertura com valor zero decimal")
    void deveRejeitarCoberturaComValorZeroDecimal() {
        // Given
        Map<String, String> coverages = Map.of("ROUBO", "0.00");

        // When
        boolean result = validator.isValid(coverages, context);

        // Then
        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Valor da cobertura 'ROUBO' deve ser maior que zero");
    }

    @Test
    @DisplayName("Deve rejeitar cobertura com valor negativo")
    void deveRejeitarCoberturaComValorNegativo() {
        // Given
        Map<String, String> coverages = Map.of("INCENDIO", "-100.00");

        // When
        boolean result = validator.isValid(coverages, context);

        // Then
        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Valor da cobertura 'INCENDIO' deve ser maior que zero");
    }

    @Test
    @DisplayName("Deve rejeitar cobertura com valor não numérico")
    void deveRejeitarCoberturaComValorNaoNumerico() {
        // Given
        Map<String, String> coverages = Map.of("COLISAO", "abc");

        // When
        boolean result = validator.isValid(coverages, context);

        // Then
        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Valor da cobertura 'COLISAO' não é um número válido");
    }

    @Test
    @DisplayName("Deve rejeitar cobertura com valor em formato monetário brasileiro")
    void deveRejeitarCoberturaComValorEmFormatoMonetarioBrasileiro() {
        // Given
        Map<String, String> coverages = Map.of("ROUBO", "100.000,00");

        // When
        boolean result = validator.isValid(coverages, context);

        // Then
        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Valor da cobertura 'ROUBO' não é um número válido");
    }

    @Test
    @DisplayName("Deve validar múltiplas coberturas válidas")
    void deveValidarMultiplasCoberturasValidas() {
        // Given
        Map<String, String> coverages = new HashMap<>();
        coverages.put("COLISAO", "200000.00");
        coverages.put("ROUBO", "150000.00");
        coverages.put("INCENDIO", "100000.00");
        coverages.put("DANOS_TERCEIROS", "50000.00");
        coverages.put("PERDA_TOTAL", "200000.00");

        // When
        boolean result = validator.isValid(coverages, context);

        // Then
        assertThat(result).isTrue();
        verify(context, never()).disableDefaultConstraintViolation();
    }

    @Test
    @DisplayName("Deve rejeitar quando primeira cobertura é válida mas segunda é inválida")
    void deveRejeitarQuandoPrimeiraCoberturaValidaMasSegundaInvalida() {
        // Given
        Map<String, String> coverages = new HashMap<>();
        coverages.put("COLISAO", "100000.00");
        coverages.put("ROUBO", "0");

        // When
        boolean result = validator.isValid(coverages, context);

        // Then
        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Valor da cobertura 'ROUBO' deve ser maior que zero");
    }

    @Test
    @DisplayName("Deve validar valores decimais positivos válidos")
    void deveValidarValoresDecimaisPositivosValidos() {
        // Given
        Map<String, String> coverages = new HashMap<>();
        coverages.put("COBERTURA_1", "0.01");
        coverages.put("COBERTURA_2", "999999.99");
        coverages.put("COBERTURA_3", "1");

        // When
        boolean result = validator.isValid(coverages, context);

        // Then
        assertThat(result).isTrue();
        verify(context, never()).disableDefaultConstraintViolation();
    }

    @Test
    @DisplayName("Deve validar coberturas com nomes especiais")
    void deveValidarCoberturasComNomesEspeciais() {
        // Given
        Map<String, String> coverages = new HashMap<>();
        coverages.put("Cobertura Básica", "100000.00");
        coverages.put("Cobertura-Premium", "200000.00");
        coverages.put("COBERTURA_123", "150000.00");

        // When
        boolean result = validator.isValid(coverages, context);

        // Then
        assertThat(result).isTrue();
        verify(context, never()).disableDefaultConstraintViolation();
    }
}
