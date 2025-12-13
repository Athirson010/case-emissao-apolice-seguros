package io.github.athirson010.adapters.out.persistence.mongo.mapper;

import io.github.athirson010.adapters.out.persistence.mongo.document.VidaDataEntity;
import org.springframework.stereotype.Component;

@Component
public class VidaDataEntityMapper {

    public VidaDataEntity toEntity(Object domainData) {
        if (domainData == null) {
            return null;
        }

        // TODO: Implementar conversão completa quando o Value Object de domínio for criado
        // Por enquanto, assumindo que domainData é um Map ou objeto similar
        throw new UnsupportedOperationException("VidaData domain to entity conversion not yet implemented");
    }

    public Object toDomain(VidaDataEntity entity) {
        if (entity == null) {
            return null;
        }

        // TODO: Implementar conversão completa quando o Value Object de domínio for criado
        throw new UnsupportedOperationException("VidaData entity to domain conversion not yet implemented");
    }
}
