package io.github.athirson010.adapters.out.persistence.mongo.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoneyEntity {

    private BigDecimal amount;
    private String currency;
}
