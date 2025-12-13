package io.github.athirson010.adapters.out.persistence.mongo.document;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class VidaDataEntity extends CategorySpecificDataEntity {

    private String insuredPersonName;
    private String insuredPersonDocument;
    private LocalDate birthDate;
    private String occupation;

    public VidaDataEntity(String insuredPersonName, String insuredPersonDocument,
                          LocalDate birthDate, String occupation) {
        super();
        setType("VIDA");
        this.insuredPersonName = insuredPersonName;
        this.insuredPersonDocument = insuredPersonDocument;
        this.birthDate = birthDate;
        this.occupation = occupation;
    }
}
