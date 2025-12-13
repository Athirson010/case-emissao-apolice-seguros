package io.github.athirson010.adapters.out.persistence.mongo.document;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ResidencialDataEntity extends CategorySpecificDataEntity {

    private String propertyType;
    private AddressEntity propertyAddress;
    private MoneyEntity propertyValue;
    private String constructionType;

    public ResidencialDataEntity(String propertyType, AddressEntity propertyAddress,
                                 MoneyEntity propertyValue, String constructionType) {
        super();
        setType("RESIDENCIAL");
        this.propertyType = propertyType;
        this.propertyAddress = propertyAddress;
        this.propertyValue = propertyValue;
        this.constructionType = constructionType;
    }
}
