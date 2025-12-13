package io.github.athirson010.adapters.out.persistence.mongo.mapper;

import io.github.athirson010.adapters.out.persistence.mongo.document.AutoDataEntity;
import org.springframework.stereotype.Component;

@Component
public class AutoDataEntityMapper {

    public AutoDataEntity toEntity(Object domainData) {
        if (domainData == null) {
            return null;
        }

        // TODO: Implementar conversão completa quando o Value Object de domínio for criado
        throw new UnsupportedOperationException("AutoData domain to entity conversion not yet implemented");
    }

    public Object toDomain(AutoDataEntity entity) {
        if (entity == null) {
            return null;
        }

        // TODO: Implementar conversão completa quando o Value Object de domínio for criado
        throw new UnsupportedOperationException("AutoData entity to domain conversion not yet implemented");
    }
}
