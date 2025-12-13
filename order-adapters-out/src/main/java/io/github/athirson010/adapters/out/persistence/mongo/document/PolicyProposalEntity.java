package io.github.athirson010.adapters.out.persistence.mongo.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "policy_proposals")
@CompoundIndexes({
        @CompoundIndex(name = "customer_status_idx", def = "{'customerId': 1, 'status': 1}")
})
public class PolicyProposalEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private String proposalNumber;

    @Indexed
    private String customerId;

    private String productId;

    private String category;

    private MoneyEntity insuredAmount;

    private MoneyEntity totalMonthlyPremiumAmount;

    private List<CoverageEntity> coverages;

    private List<AssistanceEntity> assistances;

    private String salesChannel;

    private String paymentMethod;

    private String customerRiskProfile;

    @Indexed
    private String status;

    private CategorySpecificDataEntity categorySpecificData;

    @Indexed
    private Instant createdAt;

    private Instant validatedAt;

    private Instant finishedAt;

    private Instant canceledAt;

    private List<StatusHistoryEntryEntity> statusHistory;
}
