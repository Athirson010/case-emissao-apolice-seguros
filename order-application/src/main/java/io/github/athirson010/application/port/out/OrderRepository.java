package io.github.athirson010.application.port.out;

import io.github.athirson010.domain.model.PolicyRequest;
import io.github.athirson010.domain.model.PolicyRequestId;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    PolicyRequest save(PolicyRequest policyRequest);

    Optional<PolicyRequest> findById(PolicyRequestId id);

    Optional<PolicyRequest> findByCustomerId(UUID customerId);

    void deleteById(PolicyRequestId id);

    boolean existsById(PolicyRequestId id);
}
