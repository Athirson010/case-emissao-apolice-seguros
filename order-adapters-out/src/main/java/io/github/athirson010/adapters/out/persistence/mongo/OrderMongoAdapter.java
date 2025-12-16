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

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMongoAdapter implements OrderRepository {

    private final PolicyProposalMongoRepository mongoRepository;
    private final PolicyProposalEntityMapper mapper;

    @Override
    public PolicyProposal save(PolicyProposal policyProposal) {
        log.debug("Salvando proposta de apólice com ID: {}", policyProposal.getId().asString());

        PolicyProposalEntity entity = mapper.toEntity(policyProposal);
        PolicyProposalEntity savedEntity = mongoRepository.save(entity);

        log.info("Proposta de apólice salva com sucesso com ID: {}", savedEntity.getId());
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<PolicyProposal> findById(PolicyProposalId id) {
        log.debug("Buscando proposta de apólice por ID: {}", id.asString());

        return mongoRepository.findById(id.asString())
                .map(mapper::toDomain);
    }
}
