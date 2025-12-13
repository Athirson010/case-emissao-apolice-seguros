package io.github.athirson010.core.service;

import io.github.athirson010.core.port.in.CreateOrderUseCase;
import io.github.athirson010.core.port.out.OrderRepository;
import io.github.athirson010.domain.model.PolicyProposal;
import io.github.athirson010.domain.model.PolicyProposalId;
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
    public PolicyProposal createPolicyRequest(PolicyProposal policyProposal) {
        log.info("Creating policy proposal for customer: {}", policyProposal.getCustomerId());

        PolicyProposal savedPolicy = orderRepository.save(policyProposal);

        log.info("Policy proposal created with ID: {}", savedPolicy.getId().asString());
        return savedPolicy;
    }

    @Override
    public Optional<PolicyProposal> findPolicyRequestById(PolicyProposalId id) {
        log.debug("Finding policy proposal by ID: {}", id.asString());
        return orderRepository.findById(id);
    }

    @Override
    public Optional<PolicyProposal> findPolicyRequestByCustomerId(UUID customerId) {
        log.debug("Finding policy proposal by customer ID: {}", customerId);
        return orderRepository.findByCustomerId(customerId);
    }

    @Override
    @Transactional
    public PolicyProposal cancelPolicyRequest(PolicyProposalId id, String reason) {
        log.info("Cancelling policy proposal: {}", id.asString());

        PolicyProposal policyProposal = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Policy proposal not found: " + id.asString()));

        policyProposal.cancel(reason, Instant.now());

        PolicyProposal savedPolicy = orderRepository.save(policyProposal);

        log.info("Policy proposal cancelled: {}", savedPolicy.getId().asString());
        return savedPolicy;
    }
}
