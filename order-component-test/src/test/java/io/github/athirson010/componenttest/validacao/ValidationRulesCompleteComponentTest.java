package io.github.athirson010.componenttest.validacao;

import io.github.athirson010.componenttest.BaseComponentTest;
import io.github.athirson010.componenttest.templates.PolicyRequestTemplateBuilder;
import io.github.athirson010.core.service.PolicyValidationService;
import io.github.athirson010.domain.enums.Category;
import io.github.athirson010.domain.enums.PaymentMethod;
import io.github.athirson010.domain.enums.RiskClassification;
import io.github.athirson010.domain.enums.SalesChannel;
import io.github.athirson010.domain.model.Money;
import io.github.athirson010.domain.model.PolicyProposal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de componente que garante 100% de cobertura das regras de validação
 * definidas no validation-rules.json
 *
 * Valida todas as 16 combinações:
 * - 4 Classificações de Risco (REGULAR, HIGH_RISK, PREFERENTIAL, NO_INFORMATION)
 * - 4 Categorias principais (AUTO, VIDA, RESIDENCIAL, EMPRESARIAL)
 * - 1 Categoria adicional (OUTROS)
 *
 * Cada teste valida:
 * 1. Valor DENTRO do limite → deve APROVAR
 * 2. Valor NO limite → deve APROVAR
 * 3. Valor ACIMA do limite → deve REJEITAR
 */
@ActiveProfiles({"test", "order-consumer"})
@DisplayName("Regras de Validação Completas - 100% Cobertura validation-rules.json")
public class ValidationRulesCompleteComponentTest extends BaseComponentTest {

    @Autowired
    private PolicyValidationService validationService;

    // ==========================================
    // REGULAR CUSTOMER - 5 categorias
    // ==========================================

    @ParameterizedTest(name = "[REGULAR] {0}: {1} deve ser {2}")
    @MethodSource("regularCustomerTestCases")
    @DisplayName("Cliente REGULAR - Validação de limites por categoria")
    void deveValidarLimitesClienteRegular(Category category, BigDecimal insuredAmount, boolean expectedValid, String description) {
        // Given
        PolicyProposal policy = createPolicy(category, insuredAmount);

        // When
        boolean isValid = validationService.validatePolicy(policy, RiskClassification.REGULAR);

        // Then
        assertThat(isValid)
                .as(description)
                .isEqualTo(expectedValid);
    }

    private static Stream<Arguments> regularCustomerTestCases() {
        return Stream.of(
                // VIDA e RESIDENCIAL - Limite: <= 500.000
                Arguments.of(Category.VIDA, new BigDecimal("400000.00"), true, "VIDA - Dentro do limite"),
                Arguments.of(Category.VIDA, new BigDecimal("500000.00"), true, "VIDA - No limite exato"),
                Arguments.of(Category.VIDA, new BigDecimal("500000.01"), false, "VIDA - Acima do limite"),

                Arguments.of(Category.RESIDENCIAL, new BigDecimal("400000.00"), true, "RESIDENCIAL - Dentro do limite"),
                Arguments.of(Category.RESIDENCIAL, new BigDecimal("500000.00"), true, "RESIDENCIAL - No limite exato"),
                Arguments.of(Category.RESIDENCIAL, new BigDecimal("500000.01"), false, "RESIDENCIAL - Acima do limite"),

                // AUTO - Limite: <= 350.000
                Arguments.of(Category.AUTO, new BigDecimal("200000.00"), true, "AUTO - Dentro do limite"),
                Arguments.of(Category.AUTO, new BigDecimal("350000.00"), true, "AUTO - No limite exato"),
                Arguments.of(Category.AUTO, new BigDecimal("350000.01"), false, "AUTO - Acima do limite"),

                // EMPRESARIAL - Limite: <= 255.000
                Arguments.of(Category.EMPRESARIAL, new BigDecimal("200000.00"), true, "EMPRESARIAL - Dentro do limite"),
                Arguments.of(Category.EMPRESARIAL, new BigDecimal("255000.00"), true, "EMPRESARIAL - No limite exato"),
                Arguments.of(Category.EMPRESARIAL, new BigDecimal("255000.01"), false, "EMPRESARIAL - Acima do limite"),

                // OUTROS - Limite: <= 100.000
                Arguments.of(Category.OUTROS, new BigDecimal("50000.00"), true, "OUTROS - Dentro do limite"),
                Arguments.of(Category.OUTROS, new BigDecimal("100000.00"), true, "OUTROS - No limite exato"),
                Arguments.of(Category.OUTROS, new BigDecimal("100000.01"), false, "OUTROS - Acima do limite")
        );
    }

    // ==========================================
    // HIGH_RISK CUSTOMER - 5 categorias
    // ==========================================

    @ParameterizedTest(name = "[HIGH_RISK] {0}: {1} deve ser {2}")
    @MethodSource("highRiskCustomerTestCases")
    @DisplayName("Cliente HIGH_RISK - Validação de limites por categoria")
    void deveValidarLimitesClienteHighRisk(Category category, BigDecimal insuredAmount, boolean expectedValid, String description) {
        // Given
        PolicyProposal policy = createPolicy(category, insuredAmount);

        // When
        boolean isValid = validationService.validatePolicy(policy, RiskClassification.HIGH_RISK);

        // Then
        assertThat(isValid)
                .as(description)
                .isEqualTo(expectedValid);
    }

    private static Stream<Arguments> highRiskCustomerTestCases() {
        return Stream.of(
                // AUTO - Limite: <= 250.000
                Arguments.of(Category.AUTO, new BigDecimal("150000.00"), true, "AUTO - Dentro do limite"),
                Arguments.of(Category.AUTO, new BigDecimal("250000.00"), true, "AUTO - No limite exato"),
                Arguments.of(Category.AUTO, new BigDecimal("250000.01"), false, "AUTO - Acima do limite"),

                // RESIDENCIAL - Limite: <= 150.000
                Arguments.of(Category.RESIDENCIAL, new BigDecimal("100000.00"), true, "RESIDENCIAL - Dentro do limite"),
                Arguments.of(Category.RESIDENCIAL, new BigDecimal("150000.00"), true, "RESIDENCIAL - No limite exato"),
                Arguments.of(Category.RESIDENCIAL, new BigDecimal("150000.01"), false, "RESIDENCIAL - Acima do limite"),

                // VIDA e EMPRESARIAL - Limite: <= 125.000
                Arguments.of(Category.VIDA, new BigDecimal("100000.00"), true, "VIDA - Dentro do limite"),
                Arguments.of(Category.VIDA, new BigDecimal("125000.00"), true, "VIDA - No limite exato"),
                Arguments.of(Category.VIDA, new BigDecimal("125000.01"), false, "VIDA - Acima do limite"),

                Arguments.of(Category.EMPRESARIAL, new BigDecimal("100000.00"), true, "EMPRESARIAL - Dentro do limite"),
                Arguments.of(Category.EMPRESARIAL, new BigDecimal("125000.00"), true, "EMPRESARIAL - No limite exato"),
                Arguments.of(Category.EMPRESARIAL, new BigDecimal("125000.01"), false, "EMPRESARIAL - Acima do limite"),

                // OUTROS - Limite: <= 50.000
                Arguments.of(Category.OUTROS, new BigDecimal("30000.00"), true, "OUTROS - Dentro do limite"),
                Arguments.of(Category.OUTROS, new BigDecimal("50000.00"), true, "OUTROS - No limite exato"),
                Arguments.of(Category.OUTROS, new BigDecimal("50000.01"), false, "OUTROS - Acima do limite")
        );
    }

    // ==========================================
    // PREFERENTIAL CUSTOMER - 5 categorias
    // ==========================================

    @ParameterizedTest(name = "[PREFERENTIAL] {0}: {1} deve ser {2}")
    @MethodSource("preferentialCustomerTestCases")
    @DisplayName("Cliente PREFERENTIAL - Validação de limites por categoria")
    void deveValidarLimitesClientePreferential(Category category, BigDecimal insuredAmount, boolean expectedValid, String description) {
        // Given
        PolicyProposal policy = createPolicy(category, insuredAmount);

        // When
        boolean isValid = validationService.validatePolicy(policy, RiskClassification.PREFERENTIAL);

        // Then
        assertThat(isValid)
                .as(description)
                .isEqualTo(expectedValid);
    }

    private static Stream<Arguments> preferentialCustomerTestCases() {
        return Stream.of(
                // VIDA - Limite: < 800.000 (estritamente menor)
                Arguments.of(Category.VIDA, new BigDecimal("700000.00"), true, "VIDA - Dentro do limite"),
                Arguments.of(Category.VIDA, new BigDecimal("799999.99"), true, "VIDA - Próximo ao limite"),
                Arguments.of(Category.VIDA, new BigDecimal("800000.00"), false, "VIDA - No limite (deve rejeitar - estritamente menor)"),
                Arguments.of(Category.VIDA, new BigDecimal("800000.01"), false, "VIDA - Acima do limite"),

                // AUTO e RESIDENCIAL - Limite: < 450.000 (estritamente menor)
                Arguments.of(Category.AUTO, new BigDecimal("350000.00"), true, "AUTO - Dentro do limite"),
                Arguments.of(Category.AUTO, new BigDecimal("449999.99"), true, "AUTO - Próximo ao limite"),
                Arguments.of(Category.AUTO, new BigDecimal("450000.00"), false, "AUTO - No limite (deve rejeitar)"),

                Arguments.of(Category.RESIDENCIAL, new BigDecimal("350000.00"), true, "RESIDENCIAL - Dentro do limite"),
                Arguments.of(Category.RESIDENCIAL, new BigDecimal("449999.99"), true, "RESIDENCIAL - Próximo ao limite"),
                Arguments.of(Category.RESIDENCIAL, new BigDecimal("450000.00"), false, "RESIDENCIAL - No limite (deve rejeitar)"),

                // EMPRESARIAL - Limite: <= 375.000
                Arguments.of(Category.EMPRESARIAL, new BigDecimal("300000.00"), true, "EMPRESARIAL - Dentro do limite"),
                Arguments.of(Category.EMPRESARIAL, new BigDecimal("375000.00"), true, "EMPRESARIAL - No limite exato"),
                Arguments.of(Category.EMPRESARIAL, new BigDecimal("375000.01"), false, "EMPRESARIAL - Acima do limite"),

                // OUTROS - Limite: <= 300.000
                Arguments.of(Category.OUTROS, new BigDecimal("200000.00"), true, "OUTROS - Dentro do limite"),
                Arguments.of(Category.OUTROS, new BigDecimal("300000.00"), true, "OUTROS - No limite exato"),
                Arguments.of(Category.OUTROS, new BigDecimal("300000.01"), false, "OUTROS - Acima do limite")
        );
    }

    // ==========================================
    // NO_INFORMATION CUSTOMER - 5 categorias
    // ==========================================

    @ParameterizedTest(name = "[NO_INFORMATION] {0}: {1} deve ser {2}")
    @MethodSource("noInformationCustomerTestCases")
    @DisplayName("Cliente NO_INFORMATION - Validação de limites por categoria")
    void deveValidarLimitesClienteNoInformation(Category category, BigDecimal insuredAmount, boolean expectedValid, String description) {
        // Given
        PolicyProposal policy = createPolicy(category, insuredAmount);

        // When
        boolean isValid = validationService.validatePolicy(policy, RiskClassification.NO_INFORMATION);

        // Then
        assertThat(isValid)
                .as(description)
                .isEqualTo(expectedValid);
    }

    private static Stream<Arguments> noInformationCustomerTestCases() {
        return Stream.of(
                // VIDA e RESIDENCIAL - Limite: <= 200.000
                Arguments.of(Category.VIDA, new BigDecimal("150000.00"), true, "VIDA - Dentro do limite"),
                Arguments.of(Category.VIDA, new BigDecimal("200000.00"), true, "VIDA - No limite exato"),
                Arguments.of(Category.VIDA, new BigDecimal("200000.01"), false, "VIDA - Acima do limite"),

                Arguments.of(Category.RESIDENCIAL, new BigDecimal("150000.00"), true, "RESIDENCIAL - Dentro do limite"),
                Arguments.of(Category.RESIDENCIAL, new BigDecimal("200000.00"), true, "RESIDENCIAL - No limite exato"),
                Arguments.of(Category.RESIDENCIAL, new BigDecimal("200000.01"), false, "RESIDENCIAL - Acima do limite"),

                // AUTO - Limite: <= 75.000
                Arguments.of(Category.AUTO, new BigDecimal("50000.00"), true, "AUTO - Dentro do limite"),
                Arguments.of(Category.AUTO, new BigDecimal("75000.00"), true, "AUTO - No limite exato"),
                Arguments.of(Category.AUTO, new BigDecimal("75000.01"), false, "AUTO - Acima do limite"),

                // EMPRESARIAL - Limite: <= 55.000
                Arguments.of(Category.EMPRESARIAL, new BigDecimal("40000.00"), true, "EMPRESARIAL - Dentro do limite"),
                Arguments.of(Category.EMPRESARIAL, new BigDecimal("55000.00"), true, "EMPRESARIAL - No limite exato"),
                Arguments.of(Category.EMPRESARIAL, new BigDecimal("55000.01"), false, "EMPRESARIAL - Acima do limite"),

                // OUTROS - Limite: <= 30.000
                Arguments.of(Category.OUTROS, new BigDecimal("20000.00"), true, "OUTROS - Dentro do limite"),
                Arguments.of(Category.OUTROS, new BigDecimal("30000.00"), true, "OUTROS - No limite exato"),
                Arguments.of(Category.OUTROS, new BigDecimal("30000.01"), false, "OUTROS - Acima do limite")
        );
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private PolicyProposal createPolicy(Category category, BigDecimal insuredAmount) {
        return PolicyProposal.create(
                UUID.randomUUID(),
                "PROD-TEST-2024",
                category,
                SalesChannel.MOBILE,
                PaymentMethod.CREDIT_CARD,
                Money.brl(new BigDecimal("350.00")),
                Money.brl(insuredAmount),
                Map.of("COBERTURA_BASICA", Money.brl(insuredAmount)),
                List.of("ASSISTENCIA_BASICA"),
                Instant.now()
        );
    }

    // ==========================================
    // TESTES ADICIONAIS DE EDGE CASES
    // ==========================================

    @Test
    @DisplayName("Deve validar corretamente valores decimais próximos ao limite")
    void deveValidarValoresDecimaisProximosAoLimite() {
        // Given: Valor 0.01 abaixo do limite para REGULAR AUTO (350.000)
        PolicyProposal policyAbaixo = createPolicy(Category.AUTO, new BigDecimal("349999.99"));

        // When
        boolean validAbaixo = validationService.validatePolicy(policyAbaixo, RiskClassification.REGULAR);

        // Then
        assertThat(validAbaixo).isTrue();

        // Given: Valor exatamente no limite
        PolicyProposal policyNoLimite = createPolicy(Category.AUTO, new BigDecimal("350000.00"));

        // When
        boolean validNoLimite = validationService.validatePolicy(policyNoLimite, RiskClassification.REGULAR);

        // Then
        assertThat(validNoLimite).isTrue();

        // Given: Valor 0.01 acima do limite
        PolicyProposal policyAcima = createPolicy(Category.AUTO, new BigDecimal("350000.01"));

        // When
        boolean validAcima = validationService.validatePolicy(policyAcima, RiskClassification.REGULAR);

        // Then
        assertThat(validAcima).isFalse();
    }

    @Test
    @DisplayName("Deve validar corretamente comparação estritamente menor (<) para PREFERENTIAL")
    void deveValidarComparacaoEstritamenteMenorParaPreferential() {
        // Given: VIDA PREFERENTIAL - Limite < 800.000 (estritamente menor)
        PolicyProposal policyNoLimite = createPolicy(Category.VIDA, new BigDecimal("800000.00"));

        // When
        boolean validNoLimite = validationService.validatePolicy(policyNoLimite, RiskClassification.PREFERENTIAL);

        // Then: Deve REJEITAR porque é < e não <=
        assertThat(validNoLimite).isFalse();

        // Given: Um centavo abaixo
        PolicyProposal policyAbaixo = createPolicy(Category.VIDA, new BigDecimal("799999.99"));

        // When
        boolean validAbaixo = validationService.validatePolicy(policyAbaixo, RiskClassification.PREFERENTIAL);

        // Then: Deve APROVAR
        assertThat(validAbaixo).isTrue();
    }
}
