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

    /**
     * Factory method para criar uma nova proposta de apólice no estado inicial RECEIVED.
     * Inicializa o histórico com a entrada RECEIVED.
     *
     * @param customerId                ID do cliente solicitante
     * @param productId                 ID do produto de seguro
     * @param category                  categoria do seguro (AUTO, VIDA, RESIDENCIAL, EMPRESARIAL, OUTROS)
     * @param salesChannel              canal de vendas (MOBILE, WEB, WHATSAPP, OUTROS)
     * @param paymentMethod             forma de pagamento (CREDIT_CARD, DEBIT, BOLETO, PIX)
     * @param totalMonthlyPremiumAmount valor do prêmio mensal total
     * @param insuredAmount             valor do capital segurado
     * @param coverages                 coberturas da apólice (nome da cobertura e valor)
     * @param assistances               assistências incluídas na apólice
     * @param now                       timestamp de criação
     * @return nova instância de PolicyProposal no estado RECEIVED
     */
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

    /**
     * Cancela a proposta de apólice.
     * Regra de negócio: Cancelamento só é permitido antes de estados finais (APPROVED, REJECTED, CANCELED).
     *
     * @param reason motivo do cancelamento
     * @param now    timestamp do cancelamento
     * @throws InvalidTransitionException se a proposta já estiver em estado final
     */
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

    /**
     * Valida a proposta de apólice após análise de fraude.
     * Transição válida: RECEIVED → VALIDATED
     *
     * @param now timestamp da validação
     * @throws InvalidTransitionException se a transição não for válida
     */
    public void validate(Instant now) {
        validateTransition(PolicyStatus.VALIDATED);
        this.status = PolicyStatus.VALIDATED;
        addHistoryEntry(PolicyStatus.VALIDATED, now, null);
    }

    /**
     * Marca a proposta como PENDING, aguardando confirmação de pagamento e subscrição.
     * Transição válida: VALIDATED → PENDING
     *
     * @param now timestamp da mudança de status
     * @throws InvalidTransitionException se a transição não for válida
     */
    public void markAsPending(Instant now) {
        validateTransition(PolicyStatus.PENDING);
        this.status = PolicyStatus.PENDING;
        addHistoryEntry(PolicyStatus.PENDING, now, null);
    }

    /**
     * Processa resposta do microserviço de pagamento.
     * NOVA LÓGICA:
     * - Se REJEITADO: muda status para REJECTED imediatamente
     * - Se APROVADO: só aprova se subscription também foi aprovada, senão aguarda
     * - Sempre registra no histórico, mesmo se já estiver REJECTED
     *
     * @param approved        indica se o pagamento foi aprovado
     * @param rejectionReason motivo da rejeição (se houver)
     * @param now             timestamp da resposta
     */
    public void processPaymentResponse(boolean approved, String rejectionReason, Instant now) {
        // Se já está rejeitado, apenas adiciona histórico da resposta de pagamento
        if (this.status == PolicyStatus.REJECTED) {
            if (this.paymentResponseReceived) {
                throw new IllegalStateException("Payment response already received for this policy");
            }

            this.paymentResponseReceived = true;
            this.paymentConfirmed = approved;
            this.paymentRejectionReason = rejectionReason;

            // Adiciona entrada no histórico informando o resultado do pagamento
            String historyMessage = approved
                    ? "Pagamento aprovado (após rejeição por subscrição)"
                    : "Pagamento rejeitado: " + (rejectionReason != null ? rejectionReason : "Sem motivo especificado");
            addHistoryEntry(PolicyStatus.REJECTED, now, historyMessage);
            return;
        }

        // Validação normal para status PENDING
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

        // REJEIÇÃO IMEDIATA: Se pagamento rejeitado, rejeita a apólice imediatamente
        if (!approved) {
            String reason = "Pagamento rejeitado: " + (rejectionReason != null ? rejectionReason : "Sem motivo especificado");
            reject(reason, now);
            return;
        }

        // APROVAÇÃO: Só aprova se subscription também foi aprovada
        if (this.subscriptionResponseReceived && this.subscriptionConfirmed) {
            approve(now);
        }
        // Senão, permanece PENDING aguardando resposta de subscription
    }

    /**
     * Processa resposta do microserviço de subscrição/seguro.
     * NOVA LÓGICA:
     * - Se REJEITADO: muda status para REJECTED imediatamente
     * - Se APROVADO: só aprova se payment também foi aprovado, senão aguarda
     * - Sempre registra no histórico, mesmo se já estiver REJECTED
     *
     * @param approved        indica se a subscrição foi aprovada
     * @param rejectionReason motivo da rejeição (se houver)
     * @param now             timestamp da resposta
     */
    public void processSubscriptionResponse(boolean approved, String rejectionReason, Instant now) {
        // Se já está rejeitado, apenas adiciona histórico da resposta de subscrição
        if (this.status == PolicyStatus.REJECTED) {
            if (this.subscriptionResponseReceived) {
                throw new IllegalStateException("Subscription response already received for this policy");
            }

            this.subscriptionResponseReceived = true;
            this.subscriptionConfirmed = approved;
            this.subscriptionRejectionReason = rejectionReason;

            // Adiciona entrada no histórico informando o resultado da subscrição
            String historyMessage = approved
                    ? "Subscrição aprovada (após rejeição por pagamento)"
                    : "Subscrição rejeitada: " + (rejectionReason != null ? rejectionReason : "Sem motivo especificado");
            addHistoryEntry(PolicyStatus.REJECTED, now, historyMessage);
            return;
        }

        // Validação normal para status PENDING
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

        // REJEIÇÃO IMEDIATA: Se subscrição rejeitada, rejeita a apólice imediatamente
        if (!approved) {
            String reason = "Subscrição rejeitada: " + (rejectionReason != null ? rejectionReason : "Sem motivo especificado");
            reject(reason, now);
            return;
        }

        // APROVAÇÃO: Só aprova se payment também foi aprovado
        if (this.paymentResponseReceived && this.paymentConfirmed) {
            approve(now);
        }
        // Senão, permanece PENDING aguardando resposta de payment
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

    /**
     * Aprova a proposta de apólice.
     * Regra de negócio: Só aprova quando AMBAS respostas (pagamento E subscrição) foram aprovadas.
     * Transição válida: PENDING → APPROVED (estado final)
     *
     * @param now timestamp da aprovação
     * @throws InvalidTransitionException se a transição não for válida
     */
    public void approve(Instant now) {
        validateTransition(PolicyStatus.APPROVED);
        this.status = PolicyStatus.APPROVED;
        this.finishedAt = now;
        addHistoryEntry(PolicyStatus.APPROVED, now, null);
    }

    /**
     * Rejeita a proposta de apólice.
     * Regra de negócio: Rejeição IMEDIATA quando QUALQUER resposta (pagamento OU subscrição) for rejeitada.
     * Transição válida: VALIDATED → REJECTED ou PENDING → REJECTED (estado final)
     *
     * @param reason motivo da rejeição
     * @param now    timestamp da rejeição
     * @throws InvalidTransitionException se a transição não for válida
     */
    public void reject(String reason, Instant now) {
        validateTransition(PolicyStatus.REJECTED);
        this.status = PolicyStatus.REJECTED;
        this.finishedAt = now;
        addHistoryEntry(PolicyStatus.REJECTED, now, reason);
    }

    /**
     * Valida se a transição de estado é permitida pela máquina de estados.
     * Regras de transição:
     * - RECEIVED → VALIDATED ou CANCELED
     * - VALIDATED → PENDING ou REJECTED
     * - PENDING → APPROVED ou REJECTED
     * - Estados finais (APPROVED, REJECTED, CANCELED) são imutáveis
     *
     * @param targetStatus estado de destino da transição
     * @throws InvalidTransitionException se a transição não for válida
     */
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
