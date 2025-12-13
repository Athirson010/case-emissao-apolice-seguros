package io.github.athirson010.core.service;

import io.github.athirson010.core.port.in.CreateOrderUseCase;
import io.github.athirson010.core.port.out.OrderRepository;
import io.github.athirson010.domain.model.PolicyRequest;
import io.github.athirson010.domain.model.PolicyRequestId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderApplicationService implements CreateOrderUseCase {

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public PolicyRequest createPolicyRequest(PolicyRequest policyRequest) {
        log.info("Creating policy request for customer: {}", policyRequest.getCustomerId());

        PolicyRequest savedPolicy = orderRepository.save(policyRequest);

        log.info("Policy request created with ID: {}", savedPolicy.getId().asString());
        return savedPolicy;
    }

    @Override
    public Optional<PolicyRequest> findPolicyRequestById(PolicyRequestId id) {
        log.debug("Finding policy request by ID: {}", id.asString());
        return orderRepository.findById(id);
    }

    @Override
    public Optional<PolicyRequest> findPolicyRequestByCustomerId(UUID customerId) {
        log.debug("Finding policy request by customer ID: {}", customerId);
        return orderRepository.findByCustomerId(customerId);
    }

    @Override
    @Transactional
    public PolicyRequest cancelPolicyRequest(PolicyRequestId id, String reason) {
        log.info("Cancelling policy request: {}", id.asString());

        PolicyRequest policyRequest = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Policy request not found: " + id.asString()));

        policyRequest.cancel(reason, Instant.now());

        PolicyRequest savedPolicy = orderRepository.save(policyRequest);

        log.info("Policy request cancelled: {}", savedPolicy.getId().asString());
        return savedPolicy;
    }
}
