package io.github.athirson010.domain.model;

import io.github.athirson010.domain.enums.PolicyStatus;

import java.time.Instant;

public record HistoryEntry(PolicyStatus status,
                           Instant timestamp,
                           String reason) {

    public static HistoryEntry of(PolicyStatus status, Instant timestamp) {
        return new HistoryEntry(status, timestamp, null);
    }

    public static HistoryEntry of(PolicyStatus status, Instant timestamp, String reason) {
        return new HistoryEntry(status, timestamp, reason);
    }
}
