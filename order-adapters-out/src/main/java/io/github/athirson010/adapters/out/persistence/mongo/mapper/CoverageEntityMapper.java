package io.github.athirson010.adapters.out.persistence.mongo.mapper;

import io.github.athirson010.adapters.out.persistence.mongo.document.CoverageEntity;
import io.github.athirson010.domain.model.Money;
import org.springframework.stereotype.Component;

@Component
public class CoverageEntityMapper {

    private final MoneyEntityMapper moneyMapper = new MoneyEntityMapper();

    public CoverageEntity toEntity(String coverageName, Money coverageAmount) {
        if (coverageName == null || coverageAmount == null) {
            return null;
        }

        return CoverageEntity.builder()
                .coverageName(coverageName)
                .coverageAmount(moneyMapper.toEntity(coverageAmount))
                .build();
    }

    public String getCoverageName(CoverageEntity entity) {
        return entity != null ? entity.getCoverageName() : null;
    }

    public Money getCoverageAmount(CoverageEntity entity) {
        return entity != null ? moneyMapper.toDomain(entity.getCoverageAmount()) : null;
    }
}
