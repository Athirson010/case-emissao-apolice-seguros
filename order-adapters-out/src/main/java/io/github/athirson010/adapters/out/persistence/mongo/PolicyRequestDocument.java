package io.github.athirson010.adapters.out.persistence.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "policy_requests")
@CompoundIndexes({
    @CompoundIndex(name = "customer_created_idx", def = "{'customerId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "status_created_idx", def = "{'status': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "product_created_idx", def = "{'productId': 1, 'createdAt': -1}")
})
public class PolicyRequestDocument {

    @Id
    private String id;

    @Indexed
    private String customerId;

    @Indexed
    private String productId;

    private String category;

    private String salesChannel;

    private String paymentMethod;

    private BigDecimal totalMonthlyPremiumAmount;

    private String totalMonthlyPremiumCurrency;

    private BigDecimal insuredAmount;

    private String insuredAmountCurrency;

    @Builder.Default
    private Map<String, CoverageData> coverages = new HashMap<>();

    @Builder.Default
    private List<String> assistances = new ArrayList<>();

    @Indexed
    private String status;

    @Indexed
    private Instant createdAt;

    private Instant finishedAt;

    @Builder.Default
    private List<HistoryEntryData> history = new ArrayList<>();

    private FraudAnalysisData fraudAnalysis;

    @Builder.Default
    private List<OccurrenceData> occurrences = new ArrayList<>();

    private String paymentDecision;

    private PaymentInfoData paymentInfo;

    private String subscriptionDecision;

    private SubscriptionInfoData subscriptionInfo;

    @Builder.Default
    private List<OutboxEventData> outboxEvents = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoverageData {
        private BigDecimal amount;
        private String currency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryEntryData {
        private String status;
        private Instant timestamp;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FraudAnalysisData {
        private String classification;
        private Instant analyzedAt;
        @Builder.Default
        private List<OccurrenceData> occurrences = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OccurrenceData {
        private String id;
        private String productId;
        private String type;
        private String description;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInfoData {
        private String transactionId;
        private BigDecimal amount;
        private String currency;
        private Instant updatedAt;
        private String rawDetails;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscriptionInfoData {
        private String subscriptionId;
        private String productId;
        private String riskLevel;
        private Instant updatedAt;
        private String rawDetails;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutboxEventData {
        private String eventId;
        private String eventType;
        private Instant createdAt;
        @Builder.Default
        private Boolean published = false;
        @Builder.Default
        private Integer retryCount = 0;
        private String lastError;
        private String payload;
    }
}
