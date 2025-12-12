package io.github.athirson010.adapters.out.persistence.mongo;

import io.github.athirson010.application.port.out.OrderRepository;
import io.github.athirson010.domain.model.PolicyRequest;
import io.github.athirson010.domain.model.PolicyRequestId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMongoAdapter implements OrderRepository {

    private final PolicyRequestMongoRepository mongoRepository;
    private final PolicyRequestDocumentMapper mapper;

    @Override
    public PolicyRequest save(PolicyRequest policyRequest) {
        log.debug("Saving policy request with ID: {}", policyRequest.getId().asString());

        PolicyRequestDocument document = mapper.toDocument(policyRequest);
        PolicyRequestDocument savedDocument = mongoRepository.save(document);

        log.info("Policy request saved successfully with ID: {}", savedDocument.getId());
        return mapper.toDomain(savedDocument);
    }

    @Override
    public Optional<PolicyRequest> findById(PolicyRequestId id) {
        log.debug("Finding policy request by ID: {}", id.asString());

        return mongoRepository.findById(id.asString())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<PolicyRequest> findByCustomerId(UUID customerId) {
        log.debug("Finding policy request by customer ID: {}", customerId);

        return mongoRepository.findByCustomerId(customerId.toString())
                .map(mapper::toDomain);
    }

    @Override
    public void deleteById(PolicyRequestId id) {
        log.debug("Deleting policy request with ID: {}", id.asString());

        mongoRepository.deleteById(id.asString());

        log.info("Policy request deleted successfully with ID: {}", id.asString());
    }

    @Override
    public boolean existsById(PolicyRequestId id) {
        log.debug("Checking if policy request exists with ID: {}", id.asString());

        return mongoRepository.existsById(id.asString());
    }
}
