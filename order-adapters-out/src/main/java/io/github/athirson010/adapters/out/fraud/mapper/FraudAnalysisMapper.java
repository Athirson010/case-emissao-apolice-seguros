package io.github.athirson010.adapters.out.fraud.mapper;

import io.github.athirson010.adapters.out.fraud.dto.FraudAnalysisResponseDto;
import io.github.athirson010.adapters.out.fraud.dto.FraudOccurrenceDto;
import io.github.athirson010.domain.model.FraudAnalysisResult;
import io.github.athirson010.domain.model.FraudOccurrence;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class FraudAnalysisMapper {

    public FraudAnalysisResult toDomain(FraudAnalysisResponseDto dto) {
        if (dto == null) {
            return null;
        }

        return FraudAnalysisResult.builder()
                .orderId(dto.getOrderId())
                .customerId(dto.getCustomerId())
                .analyzedAt(dto.getAnalyzedAt())
                .classification(dto.getClassification())
                .occurrences(mapOccurrencesToDomain(dto.getOccurrences()))
                .build();
    }

    private List<FraudOccurrence> mapOccurrencesToDomain(List<FraudOccurrenceDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return Collections.emptyList();
        }

        return dtos.stream()
                .map(this::mapOccurrenceToDomain)
                .collect(Collectors.toList());
    }

    private FraudOccurrence mapOccurrenceToDomain(FraudOccurrenceDto dto) {
        if (dto == null) {
            return null;
        }

        return FraudOccurrence.builder()
                .id(UUID.fromString(dto.getId()))
                .productId(dto.getProductId())
                .type(dto.getType())
                .description(dto.getDescription())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}
