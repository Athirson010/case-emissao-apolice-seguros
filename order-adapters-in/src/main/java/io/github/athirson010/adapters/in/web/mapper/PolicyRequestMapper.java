package io.github.athirson010.adapters.in.web.mapper;

import io.github.athirson010.adapters.in.web.dto.CancelPolicyResponse;
import io.github.athirson010.adapters.in.web.dto.CreatePolicyRequest;
import io.github.athirson010.adapters.in.web.dto.CreatePolicyResponse;
import io.github.athirson010.domain.model.Category;
import io.github.athirson010.domain.model.Money;
import io.github.athirson010.domain.model.PaymentMethod;
import io.github.athirson010.domain.model.PolicyRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PolicyRequestMapper {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    public static PolicyRequest toDomain(CreatePolicyRequest request) {
        return PolicyRequest.create(
                UUID.fromString(request.getCustomerId()),
                request.getProductId(),
                Category.valueOf(request.getCategory().toUpperCase()),
                request.getSalesChannel(),
                PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()),
                Money.brl(request.getTotalMonthlyPremiumAmount()),
                Money.brl(request.getInsuredAmount()),
                convertCoverages(request.getCoverages()),
                request.getAssistances(),
                Instant.now()
        );
    }

    public static CreatePolicyResponse toCreateResponse(PolicyRequest policy) {
        return CreatePolicyResponse.builder()
                .policyRequestId(policy.getId().asString())
                .status(policy.getStatus().name())
                .createdAt(formatInstant(policy.getCreatedAt()))
                .build();
    }

    public static CancelPolicyResponse toCancelResponse(PolicyRequest policy) {
        return CancelPolicyResponse.builder()
                .policyRequestId(policy.getId().asString())
                .status(policy.getStatus().name())
                .finishedAt(formatInstant(policy.getFinishedAt()))
                .build();
    }

    private static Map<String, Money> convertCoverages(Map<String, BigDecimal> coverages) {
        Map<String, Money> result = new HashMap<>();
        if (coverages != null) {
            coverages.forEach((key, value) -> result.put(key, Money.brl(value)));
        }
        return result;
    }

    private static String formatInstant(Instant instant) {
        if (instant == null) {
            return null;
        }
        return ISO_FORMATTER.format(instant.atOffset(ZoneOffset.UTC));
    }
}
