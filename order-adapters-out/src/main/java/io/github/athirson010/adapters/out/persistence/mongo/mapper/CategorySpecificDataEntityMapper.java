package io.github.athirson010.adapters.out.persistence.mongo.mapper;

import io.github.athirson010.adapters.out.persistence.mongo.document.*;
import io.github.athirson010.domain.enums.Category;
import org.springframework.stereotype.Component;

@Component
public class CategorySpecificDataEntityMapper {

    private final VidaDataEntityMapper vidaMapper = new VidaDataEntityMapper();
    private final AutoDataEntityMapper autoMapper = new AutoDataEntityMapper();
    private final ResidencialDataEntityMapper residencialMapper = new ResidencialDataEntityMapper();
    private final EmpresarialDataEntityMapper empresarialMapper = new EmpresarialDataEntityMapper();

    public CategorySpecificDataEntity toEntity(Object domainData, Category category) {
        if (domainData == null) {
            return null;
        }

        return switch (category) {
            case VIDA -> vidaMapper.toEntity(domainData);
            case AUTO -> autoMapper.toEntity(domainData);
            case RESIDENCIAL -> residencialMapper.toEntity(domainData);
            case EMPRESARIAL -> empresarialMapper.toEntity(domainData);
            default -> null;
        };
    }

    public Object toDomain(CategorySpecificDataEntity entity) {
        if (entity == null) {
            return null;
        }

        String type = entity.getType();
        if (type == null) {
            return null;
        }

        return switch (type) {
            case "VIDA" -> vidaMapper.toDomain((VidaDataEntity) entity);
            case "AUTO" -> autoMapper.toDomain((AutoDataEntity) entity);
            case "RESIDENCIAL" -> residencialMapper.toDomain((ResidencialDataEntity) entity);
            case "EMPRESARIAL" -> empresarialMapper.toDomain((EmpresarialDataEntity) entity);
            default -> null;
        };
    }
}
