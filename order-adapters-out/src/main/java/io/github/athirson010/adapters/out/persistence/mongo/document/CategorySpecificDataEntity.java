package io.github.athirson010.adapters.out.persistence.mongo.document;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@Data
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = VidaDataEntity.class, name = "VIDA"),
        @JsonSubTypes.Type(value = AutoDataEntity.class, name = "AUTO"),
        @JsonSubTypes.Type(value = ResidencialDataEntity.class, name = "RESIDENCIAL"),
        @JsonSubTypes.Type(value = EmpresarialDataEntity.class, name = "EMPRESARIAL")
})
public abstract class CategorySpecificDataEntity {

    private String type;
}
