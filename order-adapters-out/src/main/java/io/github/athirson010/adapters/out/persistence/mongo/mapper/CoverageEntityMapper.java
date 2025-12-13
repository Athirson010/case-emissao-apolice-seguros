package io.github.athirson010.adapters.out.persistence.mongo.mapper;

import io.github.athirson010.adapters.out.persistence.mongo.document.CoverageEntity;
import io.github.athirson010.domain.model.Coverage;
import org.springframework.stereotype.Component;

@Component
public class CoverageEntityMapper {

    private final MoneyEntityMapper moneyMapper = new MoneyEntityMapper();

    public CoverageEntity toEntity(Coverage domain) {
        if (domain == null) {
            return null;
        }

        return CoverageEntity.builder()
                .coverageName(domain.getCoverageName())
                .coverageAmount(moneyMapper.toEntity(domain.getCoverageAmount()))
                .build();
    }

    public Coverage toDomain(CoverageEntity entity) {
        if (entity == null) {
            return null;
        }

        return Coverage.of(
                entity.getCoverageName(),
                moneyMapper.toDomain(entity.getCoverageAmount())
        );
    }
}
