package io.github.athirson010.adapters.out.persistence.mongo.mapper;

import io.github.athirson010.adapters.out.persistence.mongo.document.EmpresarialDataEntity;
import org.springframework.stereotype.Component;

@Component
public class EmpresarialDataEntityMapper {

    private final AddressEntityMapper addressMapper = new AddressEntityMapper();

    public EmpresarialDataEntity toEntity(Object domainData) {
        if (domainData == null) {
            return null;
        }

        // TODO: Implementar conversão completa quando o Value Object de domínio for criado
        throw new UnsupportedOperationException("EmpresarialData domain to entity conversion not yet implemented");
    }

    public Object toDomain(EmpresarialDataEntity entity) {
        if (entity == null) {
            return null;
        }

        // TODO: Implementar conversão completa quando o Value Object de domínio for criado
        throw new UnsupportedOperationException("EmpresarialData entity to domain conversion not yet implemented");
    }
}
