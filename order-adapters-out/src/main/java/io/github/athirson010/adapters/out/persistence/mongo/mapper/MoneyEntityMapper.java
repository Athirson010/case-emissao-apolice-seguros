package io.github.athirson010.adapters.out.persistence.mongo.mapper;

import io.github.athirson010.adapters.out.persistence.mongo.document.MoneyEntity;
import io.github.athirson010.domain.model.Money;
import org.springframework.stereotype.Component;

@Component
public class MoneyEntityMapper {

    public MoneyEntity toEntity(Money domain) {
        if (domain == null) {
            return null;
        }

        return MoneyEntity.builder()
                .amount(domain.amount())
                .currency(domain.currency())
                .build();
    }

    public Money toDomain(MoneyEntity entity) {
        if (entity == null) {
            return null;
        }

        return Money.of(entity.getAmount(), entity.getCurrency());
    }
}
