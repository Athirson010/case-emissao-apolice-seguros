package io.github.athirson010.adapters.out.persistence.mongo.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusHistoryEntryEntity {
    private String status;
    private Instant changedAt;
    private String reason;
}
