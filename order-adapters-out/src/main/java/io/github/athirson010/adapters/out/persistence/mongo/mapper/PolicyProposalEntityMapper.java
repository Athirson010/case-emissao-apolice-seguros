package io.github.athirson010.adapters.out.persistence.mongo.mapper;

import io.github.athirson010.adapters.out.persistence.mongo.document.PolicyProposalEntity;
import io.github.athirson010.domain.model.PolicyProposal;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class PolicyProposalEntityMapper {

    private final MoneyEntityMapper moneyMapper = new MoneyEntityMapper();
    private final CoverageEntityMapper coverageMapper = new CoverageEntityMapper();
    private final AssistanceEntityMapper assistanceMapper = new AssistanceEntityMapper();
    private final StatusHistoryEntryEntityMapper statusHistoryMapper = new StatusHistoryEntryEntityMapper();

    public PolicyProposalEntity toEntity(PolicyProposal domain) {
        if (domain == null) {
            return null;
        }

        return PolicyProposalEntity.builder()
                .id(domain.getId().asString())
                .proposalNumber(null) // Será gerado pelo sistema quando necessário
                .customerId(domain.getCustomerId().toString())
                .productId(domain.getProductId())
                .category(domain.getCategory().name())
                .insuredAmount(moneyMapper.toEntity(domain.getInsuredAmount()))
                .totalMonthlyPremiumAmount(moneyMapper.toEntity(domain.getTotalMonthlyPremiumAmount()))
                .coverages(domain.getCoverages().entrySet().stream()
                        .map(entry -> coverageMapper.toEntity(entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList()))
                .assistances(domain.getAssistances().stream()
                        .map(assistanceMapper::toEntity)
                        .collect(Collectors.toList()))
                .salesChannel(domain.getSalesChannel().name())
                .paymentMethod(domain.getPaymentMethod().name())
                .customerRiskProfile(null) // Campo ainda não implementado no domínio
                .status(domain.getStatus().name())
                .categorySpecificData(null) // Campo ainda não implementado no domínio
                .createdAt(domain.getCreatedAt())
                .validatedAt(null) // Campo ainda não implementado no domínio
                .finishedAt(domain.getFinishedAt())
                .canceledAt(null) // Campo ainda não implementado no domínio
                .statusHistory(domain.getHistory().stream()
                        .map(statusHistoryMapper::toEntity)
                        .collect(Collectors.toList()))
                .build();
    }

    public PolicyProposal toDomain(PolicyProposalEntity entity) {
        if (entity == null) {
            return null;
        }

        // Conversão de Entity para Domain será implementada quando necessário
        throw new UnsupportedOperationException("Domain conversion not yet implemented");
    }
}
