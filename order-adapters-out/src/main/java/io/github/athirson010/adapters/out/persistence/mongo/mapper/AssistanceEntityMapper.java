package io.github.athirson010.adapters.out.persistence.mongo.mapper;

import io.github.athirson010.adapters.out.persistence.mongo.document.AssistanceEntity;
import org.springframework.stereotype.Component;

@Component
public class AssistanceEntityMapper {

    public AssistanceEntity toEntity(String assistanceName) {
        if (assistanceName == null) {
            return null;
        }

        return AssistanceEntity.builder()
                .assistanceName(assistanceName)
                .build();
    }

    public String toDomain(AssistanceEntity entity) {
        return entity != null ? entity.getAssistanceName() : null;
    }
}
