package io.github.athirson010.core.service;

import io.github.athirson010.domain.enums.Category;
import io.github.athirson010.domain.enums.PaymentMethod;
import io.github.athirson010.domain.enums.RiskClassification;
import io.github.athirson010.domain.enums.SalesChannel;
import io.github.athirson010.domain.model.Money;
import io.github.athirson010.domain.model.PolicyProposal;
import io.github.athirson010.domain.model.PolicyProposalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PolicyValidationService - Testes Unitários")
class PolicyValidationServiceTest {

    private PolicyValidationService policyValidationService;

    @BeforeEach
    void setUp() {
        policyValidationService = new PolicyValidationService();
    }

    // ========== TESTES PARA CLIENTE REGULAR ==========

    @ParameterizedTest
    @MethodSource("provideValidRegularCustomerScenarios")
    @DisplayName("Deve aprovar apólices válidas para cliente REGULAR")
    void shouldApproveValidPoliciesForRegularCustomer(Category category, BigDecimal insuredAmount) {
        // Given
        PolicyProposal policy = createPolicy(category, insuredAmount);

        // When
        boolean result = policyValidationService.validatePolicy(policy, RiskClassification.REGULAR);

        // Then
        assertThat(result).isTrue();
    }

    private static Stream<Arguments> provideValidRegularCustomerScenarios() {
        return Stream.of(
                Arguments.of(Category.VIDA, new BigDecimal("500000.00")),
                Arguments.of(Category.RESIDENCIAL, new BigDecimal("500000.00")),
                Arguments.of(Category.AUTO, new BigDecimal("350000.00")),
                Arguments.of(Category.EMPRESARIAL, new BigDecimal("255000.00")),
                Arguments.of(Category.OUTROS, new BigDecimal("100000.00"))
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidRegularCustomerScenarios")
    @DisplayName("Deve rejeitar apólices inválidas para cliente REGULAR")
    void shouldRejectInvalidPoliciesForRegularCustomer(Category category, BigDecimal insuredAmount) {
        // Given
        PolicyProposal policy = createPolicy(category, insuredAmount);

        // When
        boolean result = policyValidationService.validatePolicy(policy, RiskClassification.REGULAR);

        // Then
        assertThat(result).isFalse();
    }

    private static Stream<Arguments> provideInvalidRegularCustomerScenarios() {
        return Stream.of(
                Arguments.of(Category.VIDA, new BigDecimal("500000.01")),
                Arguments.of(Category.RESIDENCIAL, new BigDecimal("600000.00")),
                Arguments.of(Category.AUTO, new BigDecimal("350000.01")),
                Arguments.of(Category.EMPRESARIAL, new BigDecimal("255000.01")),
                Arguments.of(Category.OUTROS, new BigDecimal("100000.01"))
        );
    }

    // ========== TESTES PARA CLIENTE HIGH_RISK ==========

    @ParameterizedTest
    @MethodSource("provideValidHighRiskCustomerScenarios")
    @DisplayName("Deve aprovar apólices válidas para cliente HIGH_RISK")
    void shouldApproveValidPoliciesForHighRiskCustomer(Category category, BigDecimal insuredAmount) {
        // Given
        PolicyProposal policy = createPolicy(category, insuredAmount);

        // When
        boolean result = policyValidationService.validatePolicy(policy, RiskClassification.HIGH_RISK);

        // Then
        assertThat(result).isTrue();
    }

    private static Stream<Arguments> provideValidHighRiskCustomerScenarios() {
        return Stream.of(
                Arguments.of(Category.AUTO, new BigDecimal("250000.00")),
                Arguments.of(Category.RESIDENCIAL, new BigDecimal("150000.00")),
                Arguments.of(Category.VIDA, new BigDecimal("125000.00")),
                Arguments.of(Category.EMPRESARIAL, new BigDecimal("125000.00")),
                Arguments.of(Category.OUTROS, new BigDecimal("50000.00"))
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidHighRiskCustomerScenarios")
    @DisplayName("Deve rejeitar apólices inválidas para cliente HIGH_RISK")
    void shouldRejectInvalidPoliciesForHighRiskCustomer(Category category, BigDecimal insuredAmount) {
        // Given
        PolicyProposal policy = createPolicy(category, insuredAmount);

        // When
        boolean result = policyValidationService.validatePolicy(policy, RiskClassification.HIGH_RISK);

        // Then
        assertThat(result).isFalse();
    }

    private static Stream<Arguments> provideInvalidHighRiskCustomerScenarios() {
        return Stream.of(
                Arguments.of(Category.AUTO, new BigDecimal("250000.01")),
                Arguments.of(Category.RESIDENCIAL, new BigDecimal("150000.01")),
                Arguments.of(Category.VIDA, new BigDecimal("125000.01")),
                Arguments.of(Category.EMPRESARIAL, new BigDecimal("125000.01")),
                Arguments.of(Category.OUTROS, new BigDecimal("50000.01"))
        );
    }

    // ========== TESTES PARA CLIENTE PREFERENTIAL ==========

    @ParameterizedTest
    @MethodSource("provideValidPreferentialCustomerScenarios")
    @DisplayName("Deve aprovar apólices válidas para cliente PREFERENTIAL")
    void shouldApproveValidPoliciesForPreferentialCustomer(Category category, BigDecimal insuredAmount) {
        // Given
        PolicyProposal policy = createPolicy(category, insuredAmount);

        // When
        boolean result = policyValidationService.validatePolicy(policy, RiskClassification.PREFERENTIAL);

        // Then
        assertThat(result).isTrue();
    }

    private static Stream<Arguments> provideValidPreferentialCustomerScenarios() {
        return Stream.of(
                Arguments.of(Category.VIDA, new BigDecimal("799999.99")),
                Arguments.of(Category.AUTO, new BigDecimal("449999.99")),
                Arguments.of(Category.RESIDENCIAL, new BigDecimal("449999.99")),
                Arguments.of(Category.EMPRESARIAL, new BigDecimal("375000.00")),
                Arguments.of(Category.OUTROS, new BigDecimal("300000.00"))
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidPreferentialCustomerScenarios")
    @DisplayName("Deve rejeitar apólices inválidas para cliente PREFERENTIAL")
    void shouldRejectInvalidPoliciesForPreferentialCustomer(Category category, BigDecimal insuredAmount) {
        // Given
        PolicyProposal policy = createPolicy(category, insuredAmount);

        // When
        boolean result = policyValidationService.validatePolicy(policy, RiskClassification.PREFERENTIAL);

        // Then
        assertThat(result).isFalse();
    }

    private static Stream<Arguments> provideInvalidPreferentialCustomerScenarios() {
        return Stream.of(
                Arguments.of(Category.VIDA, new BigDecimal("800000.00")),
                Arguments.of(Category.AUTO, new BigDecimal("450000.00")),
                Arguments.of(Category.RESIDENCIAL, new BigDecimal("450000.00")),
                Arguments.of(Category.EMPRESARIAL, new BigDecimal("375000.01")),
                Arguments.of(Category.OUTROS, new BigDecimal("300000.01"))
        );
    }

    // ========== TESTES PARA CLIENTE NO_INFORMATION ==========

    @ParameterizedTest
    @MethodSource("provideValidNoInformationCustomerScenarios")
    @DisplayName("Deve aprovar apólices válidas para cliente NO_INFORMATION")
    void shouldApproveValidPoliciesForNoInformationCustomer(Category category, BigDecimal insuredAmount) {
        // Given
        PolicyProposal policy = createPolicy(category, insuredAmount);

        // When
        boolean result = policyValidationService.validatePolicy(policy, RiskClassification.NO_INFORMATION);

        // Then
        assertThat(result).isTrue();
    }

    private static Stream<Arguments> provideValidNoInformationCustomerScenarios() {
        return Stream.of(
                Arguments.of(Category.VIDA, new BigDecimal("200000.00")),
                Arguments.of(Category.RESIDENCIAL, new BigDecimal("200000.00")),
                Arguments.of(Category.AUTO, new BigDecimal("75000.00")),
                Arguments.of(Category.EMPRESARIAL, new BigDecimal("55000.00")),
                Arguments.of(Category.OUTROS, new BigDecimal("30000.00"))
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidNoInformationCustomerScenarios")
    @DisplayName("Deve rejeitar apólices inválidas para cliente NO_INFORMATION")
    void shouldRejectInvalidPoliciesForNoInformationCustomer(Category category, BigDecimal insuredAmount) {
        // Given
        PolicyProposal policy = createPolicy(category, insuredAmount);

        // When
        boolean result = policyValidationService.validatePolicy(policy, RiskClassification.NO_INFORMATION);

        // Then
        assertThat(result).isFalse();
    }

    private static Stream<Arguments> provideInvalidNoInformationCustomerScenarios() {
        return Stream.of(
                Arguments.of(Category.VIDA, new BigDecimal("200000.01")),
                Arguments.of(Category.RESIDENCIAL, new BigDecimal("200000.01")),
                Arguments.of(Category.AUTO, new BigDecimal("75000.01")),
                Arguments.of(Category.EMPRESARIAL, new BigDecimal("55000.01")),
                Arguments.of(Category.OUTROS, new BigDecimal("30000.01"))
        );
    }

    // ========== MÉTODO AUXILIAR ==========

    private PolicyProposal createPolicy(Category category, BigDecimal insuredAmount) {
        return PolicyProposal.create(
                UUID.randomUUID(),
                "PROD-TEST-2024",
                category,
                SalesChannel.MOBILE,
                PaymentMethod.CREDIT_CARD,
                Money.brl(new BigDecimal("350.00")),
                Money.brl(insuredAmount),
                Map.of("COVERAGE", Money.brl(insuredAmount)),
                List.of("ASSISTANCE"),
                java.time.Instant.now()
        );
    }
}
