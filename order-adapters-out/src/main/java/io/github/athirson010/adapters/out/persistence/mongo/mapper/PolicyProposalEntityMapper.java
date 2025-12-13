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
    private final CategorySpecificDataEntityMapper categoryDataMapper = new CategorySpecificDataEntityMapper();

    public PolicyProposalEntity toEntity(PolicyProposal domain) {
        if (domain == null) {
            return null;
        }

        return PolicyProposalEntity.builder()
                .id(domain.getId().asString())
                .proposalNumber(domain.getProposalNumber())
                .customerId(domain.getCustomerId().toString())
                .productId(domain.getProductId())
                .category(domain.getCategory().name())
                .insuredAmount(moneyMapper.toEntity(domain.getInsuredAmount()))
                .totalMonthlyPremiumAmount(moneyMapper.toEntity(domain.getTotalMonthlyPremiumAmount()))
                .coverages(domain.getCoverages().stream()
                        .map(coverageMapper::toEntity)
                        .collect(Collectors.toList()))
                .assistances(domain.getAssistances().stream()
                        .map(assistanceMapper::toEntity)
                        .collect(Collectors.toList()))
                .salesChannel(domain.getSalesChannel().name())
                .paymentMethod(domain.getPaymentMethod().name())
                .customerRiskProfile(domain.getCustomerRiskProfile().name())
                .status(domain.getStatus().name())
                .categorySpecificData(categoryDataMapper.toEntity(
                        domain.getCategorySpecificData(),
                        domain.getCategory()
                ))
                .createdAt(domain.getCreatedAt())
                .validatedAt(domain.getValidatedAt())
                .finishedAt(domain.getFinishedAt())
                .canceledAt(domain.getCanceledAt())
                .statusHistory(domain.getStatusHistory().stream()
                        .map(statusHistoryMapper::toEntity)
                        .collect(Collectors.toList()))
                .build();
    }

    public PolicyProposal toDomain(PolicyProposalEntity entity) {
        if (entity == null) {
            return null;
        }

        // TODO: Implementar conversão completa de Entity para Domain
        // Esta conversão será implementada conforme as classes de domínio forem criadas
        throw new UnsupportedOperationException("Domain conversion not yet implemented");
    }
}
