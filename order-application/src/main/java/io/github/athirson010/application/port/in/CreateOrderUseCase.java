package io.github.athirson010.application.port.in;

import io.github.athirson010.domain.model.PolicyRequest;
import io.github.athirson010.domain.model.PolicyRequestId;

import java.util.Optional;
import java.util.UUID;

public interface CreateOrderUseCase {

    PolicyRequest createPolicyRequest(PolicyRequest policyRequest);

    Optional<PolicyRequest> findPolicyRequestById(PolicyRequestId id);

    Optional<PolicyRequest> findPolicyRequestByCustomerId(UUID customerId);

    PolicyRequest cancelPolicyRequest(PolicyRequestId id, String reason);
}
