package io.github.athirson010.adapters.out.persistence.mongo.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverageEntity {

    private String coverageName;
    private MoneyEntity coverageAmount;
}
