package io.github.athirson010.core.service;

import io.github.athirson010.core.port.in.CreateOrderUseCase;
import io.github.athirson010.core.port.out.FraudQueuePort;
import io.github.athirson010.core.port.out.OrderRepository;
import io.github.athirson010.domain.model.PolicyProposal;
import io.github.athirson010.domain.model.PolicyProposalId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Profile("api")
@RequiredArgsConstructor
public class OrderApplicationService implements CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final FraudQueuePort fraudQueuePort;

    @Override
    @Transactional
    public PolicyProposal createPolicyRequest(PolicyProposal policyProposal) {
        log.info("Criando proposta de apólice para cliente: {}", policyProposal.getCustomerId());

        PolicyProposal savedPolicy = orderRepository.save(policyProposal);

        log.info("Proposta de apólice criada com ID: {}", savedPolicy.getId().asString());

        fraudQueuePort.sendToFraudQueue(savedPolicy);
        log.info("Proposta de apólice enviada para fila de fraude: {}", savedPolicy.getId().asString());

        return savedPolicy;
    }

    @Override
    public Optional<PolicyProposal> findPolicyRequestById(PolicyProposalId id) {
        log.debug("Buscando proposta de apólice por ID: {}", id.asString());
        return orderRepository.findById(id);
    }

    @Override
    public Optional<PolicyProposal> findPolicyRequestByCustomerId(UUID customerId) {
        log.debug("Buscando proposta de apólice por ID do cliente: {}", customerId);
        return orderRepository.findByCustomerId(customerId);
    }

    @Override
    @Transactional
    public PolicyProposal cancelPolicyRequest(PolicyProposalId id, String reason) {
        log.info("Cancelando proposta de apólice: {}", id.asString());

        PolicyProposal policyProposal = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proposta de apólice não encontrada: " + id.asString()));

        policyProposal.cancel(reason, Instant.now());

        PolicyProposal savedPolicy = orderRepository.save(policyProposal);

        log.info("Proposta de apólice cancelada: {}", savedPolicy.getId().asString());
        return savedPolicy;
    }
}
