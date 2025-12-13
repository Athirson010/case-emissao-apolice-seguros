package io.github.athirson010.adapters.out.persistence.mongo.mapper;

import io.github.athirson010.adapters.out.persistence.mongo.document.StatusHistoryEntryEntity;
import io.github.athirson010.domain.model.HistoryEntry;
import io.github.athirson010.domain.model.PolicyStatus;
import org.springframework.stereotype.Component;

@Component
public class StatusHistoryEntryEntityMapper {

    public StatusHistoryEntryEntity toEntity(HistoryEntry domain) {
        if (domain == null) {
            return null;
        }

        return StatusHistoryEntryEntity.builder()
                .status(domain.status().name())
                .changedAt(domain.getChangedAt())
                .reason(domain.reason())
                .build();
    }

    public HistoryEntry toDomain(StatusHistoryEntryEntity entity) {
        if (entity == null) {
            return null;
        }

        return HistoryEntry.of(
                PolicyStatus.valueOf(entity.getStatus()),
                entity.getChangedAt(),
                entity.getReason()
        );
    }
}
