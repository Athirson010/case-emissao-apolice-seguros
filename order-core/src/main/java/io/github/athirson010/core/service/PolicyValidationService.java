package io.github.athirson010.core.service;

import io.github.athirson010.domain.enums.Category;
import io.github.athirson010.domain.enums.RiskClassification;
import io.github.athirson010.domain.model.Money;
import io.github.athirson010.domain.model.PolicyProposal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Profile("order-consumer")
@Service
public class PolicyValidationService {

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

    private boolean validateRegularCustomer(Money insuredAmount, Category category) {
        BigDecimal amount = insuredAmount.amount();

        return switch (category) {
            case VIDA, RESIDENCIAL -> amount.compareTo(new BigDecimal("500000.00")) <= 0;
            case AUTO -> amount.compareTo(new BigDecimal("350000.00")) <= 0;
            case EMPRESARIAL -> amount.compareTo(new BigDecimal("255000.00")) <= 0;
            case OUTROS -> amount.compareTo(new BigDecimal("100000.00")) <= 0;
        };
    }

    private boolean validateHighRiskCustomer(Money insuredAmount, Category category) {
        BigDecimal amount = insuredAmount.amount();

        return switch (category) {
            case AUTO -> amount.compareTo(new BigDecimal("250000.00")) <= 0;
            case RESIDENCIAL -> amount.compareTo(new BigDecimal("150000.00")) <= 0;
            case VIDA, EMPRESARIAL -> amount.compareTo(new BigDecimal("125000.00")) <= 0;
            case OUTROS -> amount.compareTo(new BigDecimal("50000.00")) <= 0;
        };
    }

    private boolean validatePreferentialCustomer(Money insuredAmount, Category category) {
        BigDecimal amount = insuredAmount.amount();

        return switch (category) {
            case VIDA -> amount.compareTo(new BigDecimal("800000.00")) < 0;
            case AUTO, RESIDENCIAL -> amount.compareTo(new BigDecimal("450000.00")) < 0;
            case EMPRESARIAL -> amount.compareTo(new BigDecimal("375000.00")) <= 0;
            case OUTROS -> amount.compareTo(new BigDecimal("300000.00")) <= 0;
        };
    }

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
