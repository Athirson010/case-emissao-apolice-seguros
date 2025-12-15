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
    private boolean paymentConfirmed = false;

    @Builder.Default
    private boolean subscriptionConfirmed = false;

    @Builder.Default
    private boolean paymentResponseReceived = false;

    @Builder.Default
    private boolean subscriptionResponseReceived = false;

    private String paymentRejectionReason;

    private String subscriptionRejectionReason;

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
        if (this.status == PolicyStatus.CANCELED ||
                this.status == PolicyStatus.REJECTED ||
                this.status == PolicyStatus.APPROVED) {
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

    /**
     * Processa resposta do microserviço de pagamento.
     * Só aprova ou rejeita a apólice quando AMBAS respostas (pagamento + subscrição) forem recebidas.
     *
     * @param approved indica se o pagamento foi aprovado
     * @param rejectionReason motivo da rejeição (se houver)
     * @param now timestamp da resposta
     */
    public void processPaymentResponse(boolean approved, String rejectionReason, Instant now) {
        if (this.status != PolicyStatus.PENDING) {
            throw new InvalidTransitionException(
                    String.format("Cannot process payment response for policy in status: %s. Must be PENDING.", this.status)
            );
        }

        if (this.paymentResponseReceived) {
            throw new IllegalStateException("Payment response already received for this policy");
        }

        this.paymentResponseReceived = true;
        this.paymentConfirmed = approved;
        this.paymentRejectionReason = rejectionReason;

        // Só decide o status final quando AMBAS respostas foram recebidas
        if (hasBothResponses()) {
            evaluateFinalStatus(now);
        }
    }

    /**
     * Processa resposta do microserviço de subscrição/seguro.
     * Só aprova ou rejeita a apólice quando AMBAS respostas (pagamento + subscrição) forem recebidas.
     *
     * @param approved indica se a subscrição foi aprovada
     * @param rejectionReason motivo da rejeição (se houver)
     * @param now timestamp da resposta
     */
    public void processSubscriptionResponse(boolean approved, String rejectionReason, Instant now) {
        if (this.status != PolicyStatus.PENDING) {
            throw new InvalidTransitionException(
                    String.format("Cannot process subscription response for policy in status: %s. Must be PENDING.", this.status)
            );
        }

        if (this.subscriptionResponseReceived) {
            throw new IllegalStateException("Subscription response already received for this policy");
        }

        this.subscriptionResponseReceived = true;
        this.subscriptionConfirmed = approved;
        this.subscriptionRejectionReason = rejectionReason;

        // Só decide o status final quando AMBAS respostas foram recebidas
        if (hasBothResponses()) {
            evaluateFinalStatus(now);
        }
    }

    /**
     * Verifica se recebeu resposta de AMBOS microserviços (pagamento E subscrição)
     */
    private boolean hasBothResponses() {
        return this.paymentResponseReceived && this.subscriptionResponseReceived;
    }

    /**
     * Avalia o status final da apólice após receber AMBAS respostas.
     * Regras:
     * - APPROVED: Somente se AMBAS foram aprovadas
     * - REJECTED: Se PELO MENOS UMA foi rejeitada
     */
    private void evaluateFinalStatus(Instant now) {
        if (this.paymentConfirmed && this.subscriptionConfirmed) {
            // Ambas aprovadas -> APPROVED
            approve(now);
        } else {
            // Pelo menos uma rejeitada -> REJECTED
            String reason = buildRejectionReason();
            reject(reason, now);
        }
    }

    /**
     * Constrói a mensagem de rejeição combinando os motivos de pagamento e subscrição
     */
    private String buildRejectionReason() {
        List<String> reasons = new ArrayList<>();

        if (!this.paymentConfirmed && this.paymentRejectionReason != null) {
            reasons.add("Pagamento rejeitado: " + this.paymentRejectionReason);
        }

        if (!this.subscriptionConfirmed && this.subscriptionRejectionReason != null) {
            reasons.add("Subscrição rejeitada: " + this.subscriptionRejectionReason);
        }

        return reasons.isEmpty() ? "Rejeitado" : String.join("; ", reasons);
    }

    /**
     * @deprecated Use processPaymentResponse() instead
     */
    @Deprecated
    public void confirmPayment(Instant now) {
        processPaymentResponse(true, null, now);
    }

    /**
     * @deprecated Use processSubscriptionResponse() instead
     */
    @Deprecated
    public void confirmSubscription(Instant now) {
        processSubscriptionResponse(true, null, now);
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
            case VALIDATED -> targetStatus == PolicyStatus.PENDING || targetStatus == PolicyStatus.REJECTED;
            case PENDING ->
                    targetStatus == PolicyStatus.APPROVED || targetStatus == PolicyStatus.REJECTED || targetStatus == PolicyStatus.PENDING;
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
