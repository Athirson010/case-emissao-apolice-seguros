package io.github.athirson010.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

@Getter
@EqualsAndHashCode
@ToString
public class HistoryEntry {

    private final PolicyStatus status;
    private final Instant timestamp;
    private final String reason;

    private HistoryEntry(PolicyStatus status, Instant timestamp, String reason) {
        this.status = status;
        this.timestamp = timestamp;
        this.reason = reason;
    }

    public static HistoryEntry of(PolicyStatus status, Instant timestamp) {
        return new HistoryEntry(status, timestamp, null);
    }

    public static HistoryEntry of(PolicyStatus status, Instant timestamp, String reason) {
        return new HistoryEntry(status, timestamp, reason);
    }
}
