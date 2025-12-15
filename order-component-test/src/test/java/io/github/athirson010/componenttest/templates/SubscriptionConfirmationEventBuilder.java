package io.github.athirson010.componenttest.templates;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Builder semântico para criação de eventos de confirmação de subscrição/seguro.
 * Usado para simular mensagens do sistema de seguros.
 */
public class SubscriptionConfirmationEventBuilder {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private String policyRequestId;
    private String subscriptionStatus = "APPROVED";
    private String subscriptionId = UUID.randomUUID().toString();
    private Instant authorizationTimestamp = Instant.now();
    private String rejectionReason;

    public SubscriptionConfirmationEventBuilder withPolicyRequestId(String policyRequestId) {
        this.policyRequestId = policyRequestId;
        return this;
    }

    public SubscriptionConfirmationEventBuilder withSubscriptionStatus(String subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
        return this;
    }

    public SubscriptionConfirmationEventBuilder withSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
        return this;
    }

    public SubscriptionConfirmationEventBuilder withAuthorizationTimestamp(Instant authorizationTimestamp) {
        this.authorizationTimestamp = authorizationTimestamp;
        return this;
    }

    public SubscriptionConfirmationEventBuilder withRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
        return this;
    }

    /**
     * Cria um evento de subscrição APROVADA
     */
    public static SubscriptionConfirmationEventBuilder approved(String policyRequestId) {
        return new SubscriptionConfirmationEventBuilder()
                .withPolicyRequestId(policyRequestId)
                .withSubscriptionStatus("APPROVED");
    }

    /**
     * Cria um evento de subscrição REJEITADA
     */
    public static SubscriptionConfirmationEventBuilder rejected(String policyRequestId, String reason) {
        return new SubscriptionConfirmationEventBuilder()
                .withPolicyRequestId(policyRequestId)
                .withSubscriptionStatus("REJECTED")
                .withRejectionReason(reason);
    }

    /**
     * Cria um evento de subscrição REJEITADA por perfil de risco
     */
    public static SubscriptionConfirmationEventBuilder rejectedHighRisk(String policyRequestId) {
        return rejected(policyRequestId, "Perfil de risco não aceito pela seguradora");
    }

    /**
     * Cria um evento de subscrição REJEITADA por documentação incompleta
     */
    public static SubscriptionConfirmationEventBuilder rejectedIncompleteDocumentation(String policyRequestId) {
        return rejected(policyRequestId, "Documentação incompleta");
    }

    public Map<String, Object> buildAsMap() {
        Map<String, Object> event = new HashMap<>();
        event.put("policy_request_id", policyRequestId);
        event.put("subscription_status", subscriptionStatus);
        event.put("subscription_id", subscriptionId);
        event.put("authorization_timestamp", authorizationTimestamp);
        if (rejectionReason != null) {
            event.put("rejection_reason", rejectionReason);
        }
        return event;
    }

    public String buildAsJson() {
        try {
            return objectMapper.writeValueAsString(buildAsMap());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao serializar SubscriptionConfirmationEvent para JSON", e);
        }
    }
}
