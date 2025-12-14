package io.github.athirson010.adapters.out.fraud;

import io.github.athirson010.adapters.out.fraud.dto.FraudAnalysisResponseDto;
import io.github.athirson010.adapters.out.fraud.dto.FraudOccurrenceDto;
import io.github.athirson010.adapters.out.fraud.mapper.FraudAnalysisMapper;
import io.github.athirson010.core.port.out.FraudCheckPort;
import io.github.athirson010.domain.enums.OccurrenceType;
import io.github.athirson010.domain.enums.RiskClassification;
import io.github.athirson010.domain.model.FraudAnalysisResult;
import io.github.athirson010.domain.model.PolicyProposal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Profile("order-consumer")
@Component
@RequiredArgsConstructor
public class FraudApiAdapter implements FraudCheckPort {

    private final FraudAnalysisMapper mapper;

    @Override
    public FraudAnalysisResult analyzeFraud(PolicyProposal policyProposal) {
        log.info("Analisando fraude para proposta de apólice: {}", policyProposal.getId().asString());

        FraudAnalysisResponseDto mockResponse = generateMockResponse(policyProposal);

        log.info("Análise de fraude concluída. Classificação: {}, Ocorrências: {}",
                mockResponse.getClassification(), mockResponse.getOccurrences().size());

        return mapper.toDomain(mockResponse);
    }

    private FraudAnalysisResponseDto generateMockResponse(PolicyProposal policyProposal) {
        UUID customerId = policyProposal.getCustomerId();
        String proposalId = policyProposal.getId().asString();

        RiskClassification classification = determineRiskClassification(customerId);
        List<FraudOccurrenceDto> occurrences = generateOccurrences(classification, policyProposal.getProductId());

        return FraudAnalysisResponseDto.builder()
                .orderId(UUID.fromString(proposalId))
                .customerId(customerId)
                .analyzedAt(Instant.now())
                .classification(classification)
                .occurrences(occurrences)
                .build();
    }

    private RiskClassification determineRiskClassification(UUID customerId) {
        int hash = Math.abs(customerId.hashCode() % 4);

        return switch (hash) {
            case 0 -> RiskClassification.REGULAR;
            case 1 -> RiskClassification.HIGH_RISK;
            case 2 -> RiskClassification.PREFERENTIAL;
            default -> RiskClassification.NO_INFORMATION;
        };
    }

    private List<FraudOccurrenceDto> generateOccurrences(RiskClassification classification, String productId) {
        List<FraudOccurrenceDto> occurrences = new ArrayList<>();

        if (classification == RiskClassification.HIGH_RISK) {
            occurrences.add(createOccurrence(
                    productId,
                    OccurrenceType.FRAUD,
                    "Attempted fraudulent transaction detected",
                    Instant.now().minusSeconds(86400)
            ));
            occurrences.add(createOccurrence(
                    productId,
                    OccurrenceType.SUSPICION,
                    "Unusual activity flagged for review",
                    Instant.now().minusSeconds(172800)
            ));
        } else if (classification == RiskClassification.NO_INFORMATION) {
            occurrences.add(createOccurrence(
                    productId,
                    OccurrenceType.SUSPICION,
                    "Customer has limited history with the insurer",
                    Instant.now().minusSeconds(259200)
            ));
        }

        return occurrences;
    }

    private FraudOccurrenceDto createOccurrence(String productId, OccurrenceType type, String description, Instant createdAt) {
        return FraudOccurrenceDto.builder()
                .id(UUID.randomUUID().toString())
                .productId(productId)
                .type(type)
                .description(description)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }
}
