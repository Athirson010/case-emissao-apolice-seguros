package io.github.athirson010.adapters.out.fraud.dto;

import io.github.athirson010.domain.enums.RiskClassification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAnalysisResponseDto {
    private UUID orderId;
    private UUID customerId;
    private Instant analyzedAt;
    private RiskClassification classification;
    private List<FraudOccurrenceDto> occurrences;
}
