package io.github.athirson010.adapters.out.persistence.mongo;

import io.github.athirson010.adapters.out.persistence.mongo.document.PolicyProposalEntity;
import io.github.athirson010.adapters.out.persistence.mongo.mapper.PolicyProposalEntityMapper;
import io.github.athirson010.adapters.out.persistence.mongo.repository.PolicyProposalMongoRepository;
import io.github.athirson010.core.port.out.OrderRepository;
import io.github.athirson010.domain.model.PolicyProposal;
import io.github.athirson010.domain.model.PolicyProposalId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMongoAdapter implements OrderRepository {

    private final PolicyProposalMongoRepository mongoRepository;
    private final PolicyProposalEntityMapper mapper;

    @Override
    public PolicyProposal save(PolicyProposal policyProposal) {
        log.debug("Saving policy proposal with ID: {}", policyProposal.getId().asString());

        PolicyProposalEntity entity = mapper.toEntity(policyProposal);
        PolicyProposalEntity savedEntity = mongoRepository.save(entity);

        log.info("Policy proposal saved successfully with ID: {}", savedEntity.getId());
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<PolicyProposal> findById(PolicyProposalId id) {
        log.debug("Finding policy proposal by ID: {}", id.asString());

        return mongoRepository.findById(id.asString())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<PolicyProposal> findByCustomerId(UUID customerId) {
        log.debug("Finding policy proposal by customer ID: {}", customerId);

        return mongoRepository.findByCustomerId(customerId.toString())
                .map(mapper::toDomain);
    }

    @Override
    public void deleteById(PolicyProposalId id) {
        log.debug("Deleting policy proposal with ID: {}", id.asString());

        mongoRepository.deleteById(id.asString());

        log.info("Policy proposal deleted successfully with ID: {}", id.asString());
    }

    @Override
    public boolean existsById(PolicyProposalId id) {
        log.debug("Checking if policy proposal exists with ID: {}", id.asString());

        return mongoRepository.existsById(id.asString());
    }
}
