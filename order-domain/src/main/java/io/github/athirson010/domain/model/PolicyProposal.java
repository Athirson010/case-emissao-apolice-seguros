package io.github.athirson010.domain.model;

import io.github.athirson010.domain.enums.Category;
import io.github.athirson010.domain.enums.PaymentMethod;
import io.github.athirson010.domain.enums.PolicyStatus;
import io.github.athirson010.domain.enums.SalesChannel;
import io.github.athirson010.domain.exception.InvalidTransitionException;
import lombok.*;

import java.time.Instant;
import java.util.*;

@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PolicyProposal {

    private PolicyProposalId id;
    private UUID customerId;
    private String productId;
    private Category category;
    private SalesChannel salesChannel;
    private PaymentMethod paymentMethod;
    private Money totalMonthlyPremiumAmount;
    private Money insuredAmount;
    private Map<String, Money> coverages;
    private List<String> assistances;

    @Builder.Default
    private PolicyStatus status = PolicyStatus.RECEIVED;

    private Instant createdAt;
    private Instant finishedAt;

    @Builder.Default
    private List<HistoryEntry> history = new ArrayList<>();

    public static PolicyProposal create(
            UUID customerId,
            String productId,
            Category category,
            SalesChannel salesChannel,
            PaymentMethod paymentMethod,
            Money totalMonthlyPremiumAmount,
            Money insuredAmount,
            Map<String, Money> coverages,
            List<String> assistances,
            Instant now
    ) {
        PolicyProposal policyProposal = PolicyProposal.builder()
                .id(PolicyProposalId.generate())
                .customerId(customerId)
                .productId(productId)
                .category(category)
                .salesChannel(salesChannel)
                .paymentMethod(paymentMethod)
                .totalMonthlyPremiumAmount(totalMonthlyPremiumAmount)
                .insuredAmount(insuredAmount)
                .coverages(Collections.unmodifiableMap(coverages))
                .assistances(List.copyOf(assistances))
                .status(PolicyStatus.RECEIVED)
                .createdAt(now)
                .build();

        policyProposal.addHistoryEntry(PolicyStatus.RECEIVED, now, null);
        return policyProposal;
    }

    public void cancel(String reason, Instant now) {
        if (this.status == PolicyStatus.CANCELED || this.status == PolicyStatus.REJECTED) {
            throw new InvalidTransitionException(
                    String.format("Cannot cancel policy proposal in final state: %s", this.status)
            );
        }

        this.status = PolicyStatus.CANCELED;
        this.finishedAt = now;
        addHistoryEntry(PolicyStatus.CANCELED, now, reason);
    }

    public void validate(Instant now) {
        validateTransition(PolicyStatus.VALIDATED);
        this.status = PolicyStatus.VALIDATED;
        addHistoryEntry(PolicyStatus.VALIDATED, now, null);
    }

    public void markAsPending(Instant now) {
        validateTransition(PolicyStatus.PENDING);
        this.status = PolicyStatus.PENDING;
        addHistoryEntry(PolicyStatus.PENDING, now, null);
    }

    public void approve(Instant now) {
        validateTransition(PolicyStatus.APPROVED);
        this.status = PolicyStatus.APPROVED;
        this.finishedAt = now;
        addHistoryEntry(PolicyStatus.APPROVED, now, null);
    }

    public void reject(String reason, Instant now) {
        validateTransition(PolicyStatus.REJECTED);
        this.status = PolicyStatus.REJECTED;
        this.finishedAt = now;
        addHistoryEntry(PolicyStatus.REJECTED, now, reason);
    }

    private void validateTransition(PolicyStatus targetStatus) {
        if (this.status.isFinalState()) {
            throw new InvalidTransitionException(this.status, targetStatus);
        }

        boolean isValid = switch (this.status) {
            case RECEIVED -> targetStatus == PolicyStatus.VALIDATED || targetStatus == PolicyStatus.CANCELED;
            case VALIDATED -> targetStatus == PolicyStatus.APPROVED || targetStatus == PolicyStatus.REJECTED;
            case PENDING -> targetStatus == PolicyStatus.APPROVED || targetStatus == PolicyStatus.REJECTED;
            default -> false;
        };

        if (!isValid) {
            throw new InvalidTransitionException(this.status, targetStatus);
        }
    }

    private void addHistoryEntry(PolicyStatus status, Instant timestamp, String reason) {
        this.history.add(HistoryEntry.of(status, timestamp, reason));
    }

    public List<HistoryEntry> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public Map<String, Money> getCoverages() {
        return Collections.unmodifiableMap(coverages);
    }

    public List<String> getAssistances() {
        return Collections.unmodifiableList(assistances);
    }
}
