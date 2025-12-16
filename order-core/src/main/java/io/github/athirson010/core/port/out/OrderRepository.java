package io.github.athirson010.core.port.out;

import io.github.athirson010.domain.model.PolicyProposal;
import io.github.athirson010.domain.model.PolicyProposalId;

import java.util.Optional;

public interface OrderRepository {

    PolicyProposal save(PolicyProposal policyProposal);

    Optional<PolicyProposal> findById(PolicyProposalId id);
}
