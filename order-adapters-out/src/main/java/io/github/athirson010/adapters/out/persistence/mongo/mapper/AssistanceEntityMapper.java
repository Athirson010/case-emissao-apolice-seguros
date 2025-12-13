package io.github.athirson010.adapters.out.persistence.mongo.mapper;

import io.github.athirson010.adapters.out.persistence.mongo.document.AssistanceEntity;
import io.github.athirson010.domain.model.Assistance;
import org.springframework.stereotype.Component;

@Component
public class AssistanceEntityMapper {

    public AssistanceEntity toEntity(Assistance domain) {
        if (domain == null) {
            return null;
        }

        return AssistanceEntity.builder()
                .assistanceName(domain.getAssistanceName())
                .build();
    }

    public Assistance toDomain(AssistanceEntity entity) {
        if (entity == null) {
            return null;
        }

        return Assistance.of(entity.getAssistanceName());
    }
}
