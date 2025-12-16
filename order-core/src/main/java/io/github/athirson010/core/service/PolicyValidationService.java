package io.github.athirson010.core.service;

import io.github.athirson010.domain.enums.Category;
import io.github.athirson010.domain.enums.RiskClassification;
import io.github.athirson010.domain.model.Money;
import io.github.athirson010.domain.model.PolicyProposal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Serviço de validação de apólices baseado nas regras de capital segurado por classificação de risco.
 * Implementa as 16 regras de validação definidas em validation-rules.json:
 * - 4 classificações de risco (REGULAR, HIGH_RISK, PREFERENTIAL, NO_INFORMATION)
 * - 5 categorias (AUTO, VIDA, RESIDENCIAL, EMPRESARIAL, OUTROS)
 * - Limites de capital segurado específicos para cada combinação
 */
@Slf4j
@Profile("order-consumer")
@Service
public class PolicyValidationService {

    /**
     * Valida uma proposta de apólice baseado na classificação de risco do cliente.
     * Aplica as regras de limite de capital segurado conforme validation-rules.json.
     *
     * @param policyProposal proposta a ser validada
     * @param classification classificação de risco do cliente
     * @return true se a proposta está dentro dos limites permitidos, false caso contrário
     */
    public boolean validatePolicy(PolicyProposal policyProposal, RiskClassification classification) {
        log.info("Validando apólice {} com classificação {}",
                policyProposal.getId().asString(), classification);

        Money insuredAmount = policyProposal.getInsuredAmount();
        Category category = policyProposal.getCategory();

        boolean isValid = switch (classification) {
            case REGULAR -> validateRegularCustomer(insuredAmount, category);
            case HIGH_RISK -> validateHighRiskCustomer(insuredAmount, category);
            case PREFERENTIAL -> validatePreferentialCustomer(insuredAmount, category);
            case NO_INFORMATION -> validateNoInformationCustomer(insuredAmount, category);
        };

        log.info("Apólice {} resultado da validação: {}", policyProposal.getId().asString(), isValid);
        return isValid;
    }

    /**
     * Valida limites para cliente REGULAR.
     * Regras (validation-rules.json):
     * - VIDA, RESIDENCIAL: <= R$ 500.000
     * - AUTO: <= R$ 350.000
     * - EMPRESARIAL: <= R$ 255.000
     * - OUTROS: <= R$ 100.000
     *
     * @param insuredAmount valor do capital segurado
     * @param category      categoria da apólice
     * @return true se dentro do limite, false se exceder
     */
    private boolean validateRegularCustomer(Money insuredAmount, Category category) {
        BigDecimal amount = insuredAmount.amount();

        return switch (category) {
            case VIDA, RESIDENCIAL -> amount.compareTo(new BigDecimal("500000.00")) <= 0;
            case AUTO -> amount.compareTo(new BigDecimal("350000.00")) <= 0;
            case EMPRESARIAL -> amount.compareTo(new BigDecimal("255000.00")) <= 0;
            case OUTROS -> amount.compareTo(new BigDecimal("100000.00")) <= 0;
        };
    }

    /**
     * Valida limites para cliente HIGH_RISK (alto risco).
     * Regras (validation-rules.json):
     * - AUTO: <= R$ 250.000
     * - RESIDENCIAL: <= R$ 150.000
     * - VIDA, EMPRESARIAL: <= R$ 125.000
     * - OUTROS: <= R$ 50.000
     *
     * @param insuredAmount valor do capital segurado
     * @param category      categoria da apólice
     * @return true se dentro do limite, false se exceder
     */
    private boolean validateHighRiskCustomer(Money insuredAmount, Category category) {
        BigDecimal amount = insuredAmount.amount();

        return switch (category) {
            case AUTO -> amount.compareTo(new BigDecimal("250000.00")) <= 0;
            case RESIDENCIAL -> amount.compareTo(new BigDecimal("150000.00")) <= 0;
            case VIDA, EMPRESARIAL -> amount.compareTo(new BigDecimal("125000.00")) <= 0;
            case OUTROS -> amount.compareTo(new BigDecimal("50000.00")) <= 0;
        };
    }

    /**
     * Valida limites para cliente PREFERENTIAL (preferencial).
     * Regras (validation-rules.json):
     * - VIDA: < R$ 800.000 (estritamente menor)
     * - AUTO, RESIDENCIAL: < R$ 450.000 (estritamente menor)
     * - EMPRESARIAL: <= R$ 375.000
     * - OUTROS: <= R$ 300.000
     * <p>
     * IMPORTANTE: Operador < (estritamente menor) para VIDA, AUTO e RESIDENCIAL.
     *
     * @param insuredAmount valor do capital segurado
     * @param category      categoria da apólice
     * @return true se dentro do limite, false se exceder
     */
    private boolean validatePreferentialCustomer(Money insuredAmount, Category category) {
        BigDecimal amount = insuredAmount.amount();

        return switch (category) {
            case VIDA -> amount.compareTo(new BigDecimal("800000.00")) < 0;
            case AUTO, RESIDENCIAL -> amount.compareTo(new BigDecimal("450000.00")) < 0;
            case EMPRESARIAL -> amount.compareTo(new BigDecimal("375000.00")) <= 0;
            case OUTROS -> amount.compareTo(new BigDecimal("300000.00")) <= 0;
        };
    }

    /**
     * Valida limites para cliente NO_INFORMATION (sem informações de histórico).
     * Regras (validation-rules.json):
     * - VIDA, RESIDENCIAL: <= R$ 200.000
     * - AUTO: <= R$ 75.000
     * - EMPRESARIAL: <= R$ 55.000
     * - OUTROS: <= R$ 30.000
     *
     * @param insuredAmount valor do capital segurado
     * @param category      categoria da apólice
     * @return true se dentro do limite, false se exceder
     */
    private boolean validateNoInformationCustomer(Money insuredAmount, Category category) {
        BigDecimal amount = insuredAmount.amount();

        return switch (category) {
            case VIDA, RESIDENCIAL -> amount.compareTo(new BigDecimal("200000.00")) <= 0;
            case AUTO -> amount.compareTo(new BigDecimal("75000.00")) <= 0;
            case EMPRESARIAL -> amount.compareTo(new BigDecimal("55000.00")) <= 0;
            case OUTROS -> amount.compareTo(new BigDecimal("30000.00")) <= 0;
        };
    }
}
