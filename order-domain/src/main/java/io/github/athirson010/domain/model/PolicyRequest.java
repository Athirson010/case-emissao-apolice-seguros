package io.github.athirson010.domain.model;

import io.github.athirson010.domain.enums.Category;
import io.github.athirson010.domain.enums.PaymentMethod;
import io.github.athirson010.domain.enums.PolicyStatus;
import io.github.athirson010.domain.exception.InvalidTransitionException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.*;

@Getter
@ToString
@Builder(access = AccessLevel.PRIVATE)
public class PolicyRequest {

    private final PolicyRequestId id;
    private final UUID customerId;
    private final String productId;
    private final Category category;
    private final String salesChannel;
    private final PaymentMethod paymentMethod;
    private final Money totalMonthlyPremiumAmount;
    private final Money insuredAmount;
    private final Map<String, Money> coverages;
    private final List<String> assistances;

    @Builder.Default
    private PolicyStatus status = PolicyStatus.RECEIVED;

    private final Instant createdAt;
    private Instant finishedAt;

    @Builder.Default
    private final List<HistoryEntry> history = new ArrayList<>();

    public static PolicyRequest create(
            UUID customerId,
            String productId,
            Category category,
            String salesChannel,
            PaymentMethod paymentMethod,
            Money totalMonthlyPremiumAmount,
            Money insuredAmount,
            Map<String, Money> coverages,
            List<String> assistances,
            Instant now
    ) {
        PolicyRequest policyRequest = PolicyRequest.builder()
                .id(PolicyRequestId.generate())
                .customerId(customerId)
                .productId(productId)
                .category(category)
                .salesChannel(salesChannel)
                .paymentMethod(paymentMethod)
                .totalMonthlyPremiumAmount(totalMonthlyPremiumAmount)
                .insuredAmount(insuredAmount)
                .coverages(Collections.unmodifiableMap(coverages))
                .assistances(Collections.unmodifiableList(new ArrayList<>(assistances)))
                .status(PolicyStatus.RECEIVED)
                .createdAt(now)
                .build();

        policyRequest.addHistoryEntry(PolicyStatus.RECEIVED, now, null);
        return policyRequest;
    }

    public static PolicyRequest restoreForDemo(
            PolicyRequestId id,
            UUID customerId,
            String productId,
            Category category,
            String salesChannel,
            PaymentMethod paymentMethod,
            Money totalMonthlyPremiumAmount,
            Money insuredAmount,
            Map<String, Money> coverages,
            List<String> assistances,
            PolicyStatus status,
            Instant createdAt,
            Instant finishedAt,
            List<HistoryEntry> history
    ) {
        return PolicyRequest.builder()
                .id(id)
                .customerId(customerId)
                .productId(productId)
                .category(category)
                .salesChannel(salesChannel)
                .paymentMethod(paymentMethod)
                .totalMonthlyPremiumAmount(totalMonthlyPremiumAmount)
                .insuredAmount(insuredAmount)
                .coverages(Collections.unmodifiableMap(coverages))
                .assistances(Collections.unmodifiableList(new ArrayList<>(assistances)))
                .status(status)
                .createdAt(createdAt)
                .finishedAt(finishedAt)
                .history(new ArrayList<>(history))
                .build();
    }

    public void cancel(String reason, Instant now) {
        if (this.status.isFinalState()) {
            throw new InvalidTransitionException(
                    String.format("Cannot cancel policy request in final state: %s", this.status)
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
            case RECEIVED -> targetStatus == PolicyStatus.VALIDATED || targetStatus == PolicyStatus.REJECTED;
            case VALIDATED -> targetStatus == PolicyStatus.PENDING || targetStatus == PolicyStatus.REJECTED;
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
