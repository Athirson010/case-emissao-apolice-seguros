package io.github.athirson010.componenttest.templates;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.*;

/**
 * Builder semântico para criação de requisições de apólice em testes de componentes.
 * Segue o padrão Builder com métodos fluentes e valores padrão sensatos.
 */
public class PolicyRequestTemplateBuilder {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String customerId = "123e4567-e89b-12d3-a456-426614174000";
    private String productId = "PROD-AUTO-2024";
    private String category = "AUTO";
    private String salesChannel = "MOBILE";
    private String paymentMethod = "CREDIT_CARD";
    private BigDecimal totalMonthlyPremiumAmount = new BigDecimal("350.00");
    private BigDecimal insuredAmount = new BigDecimal("200000.00");
    private final Map<String, BigDecimal> coverages = new HashMap<>();
    private final List<String> assistances = new ArrayList<>();

    public PolicyRequestTemplateBuilder() {
        withDefaultCoverages();
        withDefaultAssistances();
    }

    public PolicyRequestTemplateBuilder withCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public PolicyRequestTemplateBuilder withProductId(String productId) {
        this.productId = productId;
        return this;
    }

    public PolicyRequestTemplateBuilder withCategory(String category) {
        this.category = category;
        return this;
    }

    public PolicyRequestTemplateBuilder withSalesChannel(String salesChannel) {
        this.salesChannel = salesChannel;
        return this;
    }

    public PolicyRequestTemplateBuilder withPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
        return this;
    }

    public PolicyRequestTemplateBuilder withTotalMonthlyPremiumAmount(BigDecimal amount) {
        this.totalMonthlyPremiumAmount = amount;
        return this;
    }

    public PolicyRequestTemplateBuilder withInsuredAmount(BigDecimal amount) {
        this.insuredAmount = amount;
        return this;
    }

    public PolicyRequestTemplateBuilder withCoverage(String name, BigDecimal value) {
        this.coverages.put(name, value);
        return this;
    }

    public PolicyRequestTemplateBuilder clearCoverages() {
        this.coverages.clear();
        return this;
    }

    public PolicyRequestTemplateBuilder withAssistance(String assistance) {
        this.assistances.add(assistance);
        return this;
    }

    public PolicyRequestTemplateBuilder clearAssistances() {
        this.assistances.clear();
        return this;
    }

    private void withDefaultCoverages() {
        this.coverages.put("COLISAO", new BigDecimal("200000.00"));
    }

    private void withDefaultAssistances() {
        this.assistances.add("GUINCHO_24H");
    }

    /**
     * Cria uma apólice de AUTO com valores padrão REGULAR (dentro dos limites)
     */
    public static PolicyRequestTemplateBuilder autoRegular() {
        return new PolicyRequestTemplateBuilder()
                .withCategory("AUTO")
                .withInsuredAmount(new BigDecimal("200000.00")); // Limite: 350.000
    }

    /**
     * Cria uma apólice de VIDA com valores padrão REGULAR
     */
    public static PolicyRequestTemplateBuilder vidaRegular() {
        return new PolicyRequestTemplateBuilder()
                .withCategory("VIDA")
                .withInsuredAmount(new BigDecimal("400000.00")) // Limite: 500.000
                .clearCoverages()
                .withCoverage("MORTE", new BigDecimal("400000.00"));
    }

    /**
     * Cria uma apólice RESIDENCIAL com valores padrão REGULAR
     */
    public static PolicyRequestTemplateBuilder residencialRegular() {
        return new PolicyRequestTemplateBuilder()
                .withCategory("RESIDENCIAL")
                .withInsuredAmount(new BigDecimal("350000.00")) // Limite: 500.000
                .clearCoverages()
                .withCoverage("INCENDIO", new BigDecimal("350000.00"));
    }

    /**
     * Cria uma apólice EMPRESARIAL com valores padrão REGULAR
     */
    public static PolicyRequestTemplateBuilder empresarialRegular() {
        return new PolicyRequestTemplateBuilder()
                .withCategory("EMPRESARIAL")
                .withInsuredAmount(new BigDecimal("200000.00")) // Limite: 255.000
                .clearCoverages()
                .withCoverage("RESPONSABILIDADE_CIVIL", new BigDecimal("200000.00"));
    }

    /**
     * Cria uma apólice de AUTO HIGH_RISK (dentro dos limites mais restritos)
     */
    public static PolicyRequestTemplateBuilder autoHighRisk() {
        return new PolicyRequestTemplateBuilder()
                .withCategory("AUTO")
                .withInsuredAmount(new BigDecimal("150000.00")); // Limite: 250.000
    }

    /**
     * Cria uma apólice de AUTO que EXCEDE o limite para REGULAR
     */
    public static PolicyRequestTemplateBuilder autoExceedsRegularLimit() {
        return new PolicyRequestTemplateBuilder()
                .withCategory("AUTO")
                .withInsuredAmount(new BigDecimal("400000.00")); // Excede 350.000
    }

    /**
     * Cria uma apólice de VIDA que EXCEDE o limite para REGULAR
     */
    public static PolicyRequestTemplateBuilder vidaExceedsRegularLimit() {
        return new PolicyRequestTemplateBuilder()
                .withCategory("VIDA")
                .withInsuredAmount(new BigDecimal("600000.00")) // Excede 500.000
                .clearCoverages()
                .withCoverage("MORTE", new BigDecimal("600000.00"));
    }

    /**
     * Cria uma apólice PREFERENTIAL com valores altos mas dentro do limite
     */
    public static PolicyRequestTemplateBuilder vidaPreferential() {
        return new PolicyRequestTemplateBuilder()
                .withCategory("VIDA")
                .withInsuredAmount(new BigDecimal("750000.00")) // Limite: < 800.000
                .clearCoverages()
                .withCoverage("MORTE", new BigDecimal("750000.00"));
    }

    /**
     * Cria uma apólice NO_INFORMATION com valores baixos
     */
    public static PolicyRequestTemplateBuilder autoNoInformation() {
        return new PolicyRequestTemplateBuilder()
                .withCategory("AUTO")
                .withInsuredAmount(new BigDecimal("50000.00")); // Limite: 75.000
    }

    public Map<String, Object> buildAsMap() {
        Map<String, Object> request = new HashMap<>();
        request.put("customer_id", customerId);
        request.put("product_id", productId);
        request.put("category", category);
        request.put("sales_channel", salesChannel);
        request.put("payment_method", paymentMethod);
        request.put("total_monthly_premium_amount", totalMonthlyPremiumAmount);
        request.put("insured_amount", insuredAmount);
        request.put("coverages", new HashMap<>(coverages));
        request.put("assistances", new ArrayList<>(assistances));

        return request;
    }

    public String buildAsJson() {
        try {
            return objectMapper.writeValueAsString(buildAsMap());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao serializar PolicyRequest para JSON", e);
        }
    }
}
