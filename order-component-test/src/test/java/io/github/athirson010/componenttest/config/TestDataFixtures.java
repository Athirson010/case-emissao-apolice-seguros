package io.github.athirson010.componenttest.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TestDataFixtures {

    // Default test values from application-test.properties
    public static final String DEFAULT_CUSTOMER_ID = "123e4567-e89b-12d3-a456-426614174000";
    public static final String DEFAULT_PRODUCT_ID = "PROD-AUTO-2024";
    public static final String POLICY_NUMBER_PREFIX = "TEST-POL";
    public static final BigDecimal DEFAULT_PREMIUM_AMOUNT = new BigDecimal("350.00");
    public static final BigDecimal DEFAULT_INSURED_AMOUNT = new BigDecimal("200000.00");

    // Sample categories
    public static final String CATEGORY_AUTO = "AUTO";
    public static final String CATEGORY_RESIDENCE = "RESIDENCE";
    public static final String CATEGORY_LIFE = "LIFE";

    // Sample sales channels
    public static final String CHANNEL_MOBILE = "MOBILE";
    public static final String CHANNEL_WEB = "WEB";
    public static final String CHANNEL_AGENT = "AGENT";

    // Sample payment methods
    public static final String PAYMENT_CREDIT_CARD = "CREDIT_CARD";
    public static final String PAYMENT_DEBIT_CARD = "DEBIT_CARD";
    public static final String PAYMENT_BANK_SLIP = "BANK_SLIP";

    // Sample coverages
    public static final String COVERAGE_COLISAO = "COLISAO";
    public static final String COVERAGE_ROUBO = "ROUBO";
    public static final String COVERAGE_INCENDIO = "INCENDIO";

    // Sample assistances
    public static final String ASSISTANCE_GUINCHO = "GUINCHO_24H";
    public static final String ASSISTANCE_CHAVEIRO = "CHAVEIRO";
    public static final String ASSISTANCE_VIDRACEIRO = "VIDRACEIRO";

    /**
     * Creates a sample policy request JSON for API testing
     */
    public static String createSamplePolicyRequestJson() {
        return """
            {
                "customer_id": "%s",
                "product_id": "%s",
                "category": "%s",
                "sales_channel": "%s",
                "payment_method": "%s",
                "total_monthly_premium_amount": %.2f,
                "insured_amount": %.2f,
                "coverages": {
                    "%s": %.2f
                },
                "assistances": ["%s"]
            }
            """.formatted(
                DEFAULT_CUSTOMER_ID,
                DEFAULT_PRODUCT_ID,
                CATEGORY_AUTO,
                CHANNEL_MOBILE,
                PAYMENT_CREDIT_CARD,
                DEFAULT_PREMIUM_AMOUNT,
                DEFAULT_INSURED_AMOUNT,
                COVERAGE_COLISAO,
                DEFAULT_INSURED_AMOUNT,
                ASSISTANCE_GUINCHO
            );
    }

    public static Map<String, Object> createSamplePolicyRequestMap() {
        Map<String, Object> request = new HashMap<>();
        request.put("customer_id", DEFAULT_CUSTOMER_ID);
        request.put("product_id", DEFAULT_PRODUCT_ID);
        request.put("category", CATEGORY_AUTO);
        request.put("sales_channel", CHANNEL_MOBILE);
        request.put("payment_method", PAYMENT_CREDIT_CARD);
        request.put("total_monthly_premium_amount", DEFAULT_PREMIUM_AMOUNT);
        request.put("insured_amount", DEFAULT_INSURED_AMOUNT);

        Map<String, BigDecimal> coverages = new HashMap<>();
        coverages.put(COVERAGE_COLISAO, DEFAULT_INSURED_AMOUNT);
        request.put("coverages", coverages);

        request.put("assistances", List.of(ASSISTANCE_GUINCHO));

        return request;
    }

    public static String generateUniquePolicyNumber() {
        return POLICY_NUMBER_PREFIX + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public static String generateUniqueCustomerId() {
        return UUID.randomUUID().toString();
    }

    public static String createSampleInsuranceResponseJson(String policyNumber) {
        return """
            {
                "policy_number": "%s",
                "status": "APPROVED",
                "approval_date": "%s",
                "valid_until": "%s"
            }
            """.formatted(
                policyNumber,
                LocalDateTime.now(),
                LocalDateTime.now().plusYears(1)
            );
    }

    public static String createSamplePaymentResponseJson(String transactionId) {
        return """
            {
                "transaction_id": "%s",
                "status": "APPROVED",
                "amount": %.2f,
                "payment_date": "%s"
            }
            """.formatted(
                transactionId,
                DEFAULT_PREMIUM_AMOUNT,
                LocalDateTime.now()
            );
    }

    public static String createInvalidPolicyRequestJson() {
        return """
            {
                "customer_id": "",
                "product_id": null,
                "category": "INVALID_CATEGORY"
            }
            """;
    }

    public static String createPolicyRequestWithInvalidAmountsJson() {
        return """
            {
                "customer_id": "%s",
                "product_id": "%s",
                "category": "%s",
                "sales_channel": "%s",
                "payment_method": "%s",
                "total_monthly_premium_amount": -100.00,
                "insured_amount": 0.00,
                "coverages": {},
                "assistances": []
            }
            """.formatted(
                DEFAULT_CUSTOMER_ID,
                DEFAULT_PRODUCT_ID,
                CATEGORY_AUTO,
                CHANNEL_MOBILE,
                PAYMENT_CREDIT_CARD
            );
    }
}
