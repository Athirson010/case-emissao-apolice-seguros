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

        // Converte List<CoverageEntity> para Map<String, Money>
        java.util.Map<String, io.github.athirson010.domain.model.Money> coveragesMap = new java.util.HashMap<>();
        if (entity.getCoverages() != null) {
            entity.getCoverages().forEach(coverage -> {
                String name = coverageMapper.getCoverageName(coverage);
                io.github.athirson010.domain.model.Money amount = coverageMapper.getCoverageAmount(coverage);
                if (name != null && amount != null) {
                    coveragesMap.put(name, amount);
                }
            });
        }

        // Converte List<AssistanceEntity> para List<String>
        java.util.List<String> assistancesList = entity.getAssistances() != null
                ? entity.getAssistances().stream()
                    .map(assistanceMapper::toDomain)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList())
                : java.util.Collections.emptyList();

        // Converte List<StatusHistoryEntryEntity> para List<HistoryEntry>
        java.util.List<io.github.athirson010.domain.model.HistoryEntry> historyList = entity.getStatusHistory() != null
                ? entity.getStatusHistory().stream()
                    .map(statusHistoryMapper::toDomain)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList())
                : new java.util.ArrayList<>();

        return PolicyProposal.builder()
                .id(io.github.athirson010.domain.model.PolicyProposalId.from(entity.getId()))
                .customerId(java.util.UUID.fromString(entity.getCustomerId()))
                .productId(entity.getProductId())
                .category(io.github.athirson010.domain.enums.Category.valueOf(entity.getCategory()))
                .salesChannel(io.github.athirson010.domain.enums.SalesChannel.valueOf(entity.getSalesChannel()))
                .paymentMethod(io.github.athirson010.domain.enums.PaymentMethod.valueOf(entity.getPaymentMethod()))
                .totalMonthlyPremiumAmount(moneyMapper.toDomain(entity.getTotalMonthlyPremiumAmount()))
                .insuredAmount(moneyMapper.toDomain(entity.getInsuredAmount()))
                .coverages(coveragesMap)
                .assistances(assistancesList)
                .status(io.github.athirson010.domain.enums.PolicyStatus.valueOf(entity.getStatus()))
                .createdAt(entity.getCreatedAt())
                .finishedAt(entity.getFinishedAt())
                .history(historyList)
                .build();
    }
}
