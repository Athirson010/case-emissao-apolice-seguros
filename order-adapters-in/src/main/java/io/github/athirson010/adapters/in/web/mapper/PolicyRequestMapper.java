package io.github.athirson010.adapters.in.web.mapper;

import io.github.athirson010.adapters.in.web.dto.CancelPolicyResponse;
import io.github.athirson010.adapters.in.web.dto.CreatePolicyRequest;
import io.github.athirson010.adapters.in.web.dto.CreatePolicyResponse;
import io.github.athirson010.domain.enums.Category;
import io.github.athirson010.domain.enums.PaymentMethod;
import io.github.athirson010.domain.enums.SalesChannel;
import io.github.athirson010.domain.model.Money;
import io.github.athirson010.domain.model.PolicyProposal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PolicyRequestMapper {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    public static PolicyProposal toDomain(CreatePolicyRequest request) {
        return PolicyProposal.create(
                UUID.fromString(request.getCustomerId()),
                request.getProductId(),
                Category.fromString(request.getCategory()),
                SalesChannel.fromString(request.getSalesChannel()),
                PaymentMethod.fromString(request.getPaymentMethod()),
                Money.brl(new BigDecimal(request.getTotalMonthlyPremiumAmount())),
                Money.brl(new BigDecimal(request.getInsuredAmount())),
                convertCoverages(request.getCoverages()),
                request.getAssistances(),
                Instant.now()
        );
    }

    public static CreatePolicyResponse toCreateResponse(PolicyProposal policy) {
        return CreatePolicyResponse.builder()
                .policyRequestId(policy.getId().asString())
                .status(policy.getStatus().name())
                .createdAt(formatInstant(policy.getCreatedAt()))
                .build();
    }

    public static CancelPolicyResponse toCancelResponse(PolicyProposal policy) {
        return CancelPolicyResponse.builder()
                .policyRequestId(policy.getId().asString())
                .status(policy.getStatus().name())
                .finishedAt(formatInstant(policy.getFinishedAt()))
                .build();
    }

    private static Map<String, Money> convertCoverages(Map<String, String> coverages) {
        Map<String, Money> result = new HashMap<>();
        if (coverages != null) {
            coverages.forEach((key, value) -> {
                BigDecimal amount = new BigDecimal(value);
                result.put(key, Money.brl(amount));
            });
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
