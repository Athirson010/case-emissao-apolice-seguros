package io.github.athirson010.domain.model;

import io.github.athirson010.domain.enums.RiskClassification;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@ToString
@Builder
public class FraudAnalysisResult {
    private final UUID orderId;
    private final UUID customerId;
    private final Instant analyzedAt;
    private final RiskClassification classification;
    private final List<FraudOccurrence> occurrences;
}
