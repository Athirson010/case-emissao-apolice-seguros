package io.github.athirson010.adapters.out.persistence.mongo.document;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EmpresarialDataEntity extends CategorySpecificDataEntity {

    private String companyName;
    private String companyDocument;
    private String businessSegment;
    private AddressEntity companyAddress;
    private MoneyEntity companyAnnualRevenue;

    public EmpresarialDataEntity(String companyName, String companyDocument,
                                 String businessSegment, AddressEntity companyAddress,
                                 MoneyEntity companyAnnualRevenue) {
        super();
        setType("EMPRESARIAL");
        this.companyName = companyName;
        this.companyDocument = companyDocument;
        this.businessSegment = businessSegment;
        this.companyAddress = companyAddress;
        this.companyAnnualRevenue = companyAnnualRevenue;
    }
}
