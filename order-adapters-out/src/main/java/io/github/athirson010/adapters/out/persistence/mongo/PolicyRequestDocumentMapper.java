package io.github.athirson010.adapters.out.persistence.mongo;

import io.github.athirson010.domain.model.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.stream.Collectors;

@Component
public class PolicyRequestDocumentMapper {

    public PolicyRequestDocument toDocument(PolicyRequest policyRequest) {
        if (policyRequest == null) {
            return null;
        }

        return PolicyRequestDocument.builder()
                .id(policyRequest.getId().asString())
                .customerId(policyRequest.getCustomerId().toString())
                .productId(policyRequest.getProductId())
                .category(policyRequest.getCategory().name())
                .salesChannel(policyRequest.getSalesChannel())
                .paymentMethod(policyRequest.getPaymentMethod().name())
                .totalMonthlyPremiumAmount(policyRequest.getTotalMonthlyPremiumAmount().getAmount())
                .totalMonthlyPremiumCurrency(policyRequest.getTotalMonthlyPremiumAmount().getCurrency())
                .insuredAmount(policyRequest.getInsuredAmount().getAmount())
                .insuredAmountCurrency(policyRequest.getInsuredAmount().getCurrency())
                .coverages(mapCoverages(policyRequest))
                .assistances(policyRequest.getAssistances())
                .status(policyRequest.getStatus().name())
                .createdAt(policyRequest.getCreatedAt())
                .finishedAt(policyRequest.getFinishedAt())
                .history(policyRequest.getHistory().stream()
                        .map(this::toHistoryEntryData)
                        .collect(Collectors.toList()))
                .build();
    }

    public PolicyRequest toDomain(PolicyRequestDocument document) {
        if (document == null) {
            return null;
        }

        return PolicyRequest.restoreForDemo(
                PolicyRequestId.from(document.getId()),
                java.util.UUID.fromString(document.getCustomerId()),
                document.getProductId(),
                Category.valueOf(document.getCategory()),
                document.getSalesChannel(),
                PaymentMethod.valueOf(document.getPaymentMethod()),
                Money.of(document.getTotalMonthlyPremiumAmount(), document.getTotalMonthlyPremiumCurrency()),
                Money.of(document.getInsuredAmount(), document.getInsuredAmountCurrency()),
                mapCoveragesToDomain(document),
                document.getAssistances(),
                PolicyStatus.valueOf(document.getStatus()),
                document.getCreatedAt(),
                document.getFinishedAt(),
                document.getHistory().stream()
                        .map(this::toHistoryEntry)
                        .collect(Collectors.toList())
        );
    }

    private java.util.Map<String, PolicyRequestDocument.CoverageData> mapCoverages(PolicyRequest policyRequest) {
        return policyRequest.getCoverages().entrySet().stream()
                .collect(Collectors.toMap(
                        java.util.Map.Entry::getKey,
                        entry -> PolicyRequestDocument.CoverageData.builder()
                                .amount(entry.getValue().getAmount())
                                .currency(entry.getValue().getCurrency())
                                .build()
                ));
    }

    private java.util.Map<String, Money> mapCoveragesToDomain(PolicyRequestDocument document) {
        if (document.getCoverages() == null) {
            return new HashMap<>();
        }

        return document.getCoverages().entrySet().stream()
                .collect(Collectors.toMap(
                        java.util.Map.Entry::getKey,
                        entry -> Money.of(entry.getValue().getAmount(), entry.getValue().getCurrency())
                ));
    }

    private PolicyRequestDocument.HistoryEntryData toHistoryEntryData(HistoryEntry historyEntry) {
        return PolicyRequestDocument.HistoryEntryData.builder()
                .status(historyEntry.getStatus().name())
                .timestamp(historyEntry.getTimestamp())
                .source(historyEntry.getReason())
                .build();
    }

    private HistoryEntry toHistoryEntry(PolicyRequestDocument.HistoryEntryData data) {
        return HistoryEntry.of(
                PolicyStatus.valueOf(data.getStatus()),
                data.getTimestamp(),
                data.getSource()
        );
    }
}
