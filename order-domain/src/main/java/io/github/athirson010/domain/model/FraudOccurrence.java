package io.github.athirson010.domain.model;

import io.github.athirson010.domain.enums.OccurrenceType;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Getter
@ToString
@Builder
public class FraudOccurrence {
    private final UUID id;
    private final String productId;
    private final OccurrenceType type;
    private final String description;
    private final Instant createdAt;
    private final Instant updatedAt;
}
