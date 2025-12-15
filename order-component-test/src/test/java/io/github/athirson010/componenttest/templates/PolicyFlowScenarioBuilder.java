package io.github.athirson010.componenttest.templates;

/**
 * Builder para criar cenários completos de fluxo de apólice.
 * Combina criação de policy + eventos de pagamento e subscrição.
 * Facilita a criação de testes end-to-end.
 */
public class PolicyFlowScenarioBuilder {

    private final PolicyRequestTemplateBuilder policyBuilder;
    private PaymentConfirmationEventBuilder paymentBuilder;
    private SubscriptionConfirmationEventBuilder subscriptionBuilder;

    private PolicyFlowScenarioBuilder(PolicyRequestTemplateBuilder policyBuilder) {
        this.policyBuilder = policyBuilder;
    }

    /**
     * Inicia um cenário com uma policy de AUTO REGULAR
     */
    public static PolicyFlowScenarioBuilder autoRegularFlow() {
        return new PolicyFlowScenarioBuilder(PolicyRequestTemplateBuilder.autoRegular());
    }

    /**
     * Inicia um cenário com uma policy de VIDA REGULAR
     */
    public static PolicyFlowScenarioBuilder vidaRegularFlow() {
        return new PolicyFlowScenarioBuilder(PolicyRequestTemplateBuilder.vidaRegular());
    }

    /**
     * Inicia um cenário com uma policy RESIDENCIAL REGULAR
     */
    public static PolicyFlowScenarioBuilder residencialRegularFlow() {
        return new PolicyFlowScenarioBuilder(PolicyRequestTemplateBuilder.residencialRegular());
    }

    /**
     * Inicia um cenário com uma policy personalizada
     */
    public static PolicyFlowScenarioBuilder customFlow(PolicyRequestTemplateBuilder policyBuilder) {
        return new PolicyFlowScenarioBuilder(policyBuilder);
    }

    /**
     * Configura pagamento aprovado para este cenário
     */
    public PolicyFlowScenarioBuilder withPaymentApproved(String policyRequestId) {
        this.paymentBuilder = PaymentConfirmationEventBuilder.approved(policyRequestId);
        return this;
    }

    /**
     * Configura pagamento rejeitado para este cenário
     */
    public PolicyFlowScenarioBuilder withPaymentRejected(String policyRequestId, String reason) {
        this.paymentBuilder = PaymentConfirmationEventBuilder.rejected(policyRequestId, reason);
        return this;
    }

    /**
     * Configura subscrição aprovada para este cenário
     */
    public PolicyFlowScenarioBuilder withSubscriptionApproved(String policyRequestId) {
        this.subscriptionBuilder = SubscriptionConfirmationEventBuilder.approved(policyRequestId);
        return this;
    }

    /**
     * Configura subscrição rejeitada para este cenário
     */
    public PolicyFlowScenarioBuilder withSubscriptionRejected(String policyRequestId, String reason) {
        this.subscriptionBuilder = SubscriptionConfirmationEventBuilder.rejected(policyRequestId, reason);
        return this;
    }

    /**
     * Cenário de sucesso completo: policy criada, pagamento e subscrição aprovados
     */
    public static PolicyFlowScenarioBuilder successfulFlow(String policyRequestId) {
        return autoRegularFlow()
                .withPaymentApproved(policyRequestId)
                .withSubscriptionApproved(policyRequestId);
    }

    /**
     * Cenário de falha por pagamento: policy criada, pagamento rejeitado
     */
    public static PolicyFlowScenarioBuilder failedByPayment(String policyRequestId, String reason) {
        return autoRegularFlow()
                .withPaymentRejected(policyRequestId, reason);
    }

    /**
     * Cenário de falha por subscrição: policy criada, pagamento aprovado, subscrição rejeitada
     */
    public static PolicyFlowScenarioBuilder failedBySubscription(String policyRequestId, String reason) {
        return autoRegularFlow()
                .withPaymentApproved(policyRequestId)
                .withSubscriptionRejected(policyRequestId, reason);
    }

    public PolicyRequestTemplateBuilder getPolicyBuilder() {
        return policyBuilder;
    }

    public PaymentConfirmationEventBuilder getPaymentBuilder() {
        return paymentBuilder;
    }

    public SubscriptionConfirmationEventBuilder getSubscriptionBuilder() {
        return subscriptionBuilder;
    }
}
