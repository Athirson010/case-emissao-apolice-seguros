package io.github.athirson010.adapters.out.fraud.dto;

import io.github.athirson010.domain.enums.OccurrenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudOccurrenceDto {
    private String id;
    private String productId;
    private OccurrenceType type;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}
