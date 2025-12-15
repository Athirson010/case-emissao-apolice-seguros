package io.github.athirson010.componenttest.templates;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Builder semântico para criação de eventos de confirmação de pagamento.
 * Usado para simular mensagens do sistema de pagamentos.
 */
public class PaymentConfirmationEventBuilder {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private String policyRequestId;
    private String paymentStatus = "APPROVED";
    private String transactionId = UUID.randomUUID().toString();
    private String amount = "350.00";
    private String paymentMethod = "CREDIT_CARD";
    private Instant paymentTimestamp = Instant.now();
    private String rejectionReason;

    public PaymentConfirmationEventBuilder withPolicyRequestId(String policyRequestId) {
        this.policyRequestId = policyRequestId;
        return this;
    }

    public PaymentConfirmationEventBuilder withPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
        return this;
    }

    public PaymentConfirmationEventBuilder withTransactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public PaymentConfirmationEventBuilder withAmount(String amount) {
        this.amount = amount;
        return this;
    }

    public PaymentConfirmationEventBuilder withPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
        return this;
    }

    public PaymentConfirmationEventBuilder withPaymentTimestamp(Instant paymentTimestamp) {
        this.paymentTimestamp = paymentTimestamp;
        return this;
    }

    public PaymentConfirmationEventBuilder withRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
        return this;
    }

    /**
     * Cria um evento de pagamento APROVADO
     */
    public static PaymentConfirmationEventBuilder approved(String policyRequestId) {
        return new PaymentConfirmationEventBuilder()
                .withPolicyRequestId(policyRequestId)
                .withPaymentStatus("APPROVED");
    }

    /**
     * Cria um evento de pagamento REJEITADO
     */
    public static PaymentConfirmationEventBuilder rejected(String policyRequestId, String reason) {
        return new PaymentConfirmationEventBuilder()
                .withPolicyRequestId(policyRequestId)
                .withPaymentStatus("REJECTED")
                .withRejectionReason(reason);
    }

    /**
     * Cria um evento de pagamento REJEITADO por fundos insuficientes
     */
    public static PaymentConfirmationEventBuilder rejectedInsufficientFunds(String policyRequestId) {
        return rejected(policyRequestId, "Fundos insuficientes");
    }

    /**
     * Cria um evento de pagamento REJEITADO por cartão inválido
     */
    public static PaymentConfirmationEventBuilder rejectedInvalidCard(String policyRequestId) {
        return rejected(policyRequestId, "Cartão de crédito inválido");
    }

    public Map<String, Object> buildAsMap() {
        Map<String, Object> event = new HashMap<>();
        event.put("policy_request_id", policyRequestId);
        event.put("payment_status", paymentStatus);
        event.put("transaction_id", transactionId);
        event.put("amount", amount);
        event.put("payment_method", paymentMethod);
        event.put("payment_timestamp", paymentTimestamp);
        if (rejectionReason != null) {
            event.put("rejection_reason", rejectionReason);
        }
        return event;
    }

    public String buildAsJson() {
        try {
            return objectMapper.writeValueAsString(buildAsMap());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao serializar PaymentConfirmationEvent para JSON", e);
        }
    }
}
