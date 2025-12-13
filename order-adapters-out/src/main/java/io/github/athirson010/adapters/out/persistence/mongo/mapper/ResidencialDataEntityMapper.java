package io.github.athirson010.adapters.out.persistence.mongo.mapper;

import io.github.athirson010.adapters.out.persistence.mongo.document.ResidencialDataEntity;
import org.springframework.stereotype.Component;

@Component
public class ResidencialDataEntityMapper {

    private final AddressEntityMapper addressMapper = new AddressEntityMapper();

    public ResidencialDataEntity toEntity(Object domainData) {
        if (domainData == null) {
            return null;
        }

        // TODO: Implementar conversão completa quando o Value Object de domínio for criado
        throw new UnsupportedOperationException("ResidencialData domain to entity conversion not yet implemented");
    }

    public Object toDomain(ResidencialDataEntity entity) {
        if (entity == null) {
            return null;
        }

        // TODO: Implementar conversão completa quando o Value Object de domínio for criado
        throw new UnsupportedOperationException("ResidencialData entity to domain conversion not yet implemented");
    }
}
