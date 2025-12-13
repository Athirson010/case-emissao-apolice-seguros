package io.github.athirson010.adapters.out.persistence.mongo.mapper;

import io.github.athirson010.adapters.out.persistence.mongo.document.AddressEntity;
import org.springframework.stereotype.Component;

@Component
public class AddressEntityMapper {

    public AddressEntity toEntity(Object domainAddress) {
        if (domainAddress == null) {
            return null;
        }

        // TODO: Implementar conversão completa quando o Value Object Address de domínio for criado
        throw new UnsupportedOperationException("Address domain to entity conversion not yet implemented");
    }

    public Object toDomain(AddressEntity entity) {
        if (entity == null) {
            return null;
        }

        // TODO: Implementar conversão completa quando o Value Object Address de domínio for criado
        throw new UnsupportedOperationException("Address entity to domain conversion not yet implemented");
    }
}
