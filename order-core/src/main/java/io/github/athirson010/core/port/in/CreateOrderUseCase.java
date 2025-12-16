package io.github.athirson010.core.port.in;

import io.github.athirson010.domain.model.PolicyProposal;
import io.github.athirson010.domain.model.PolicyProposalId;

import java.util.Optional;

public interface CreateOrderUseCase {

    PolicyProposal createPolicyRequest(PolicyProposal policyProposal);

    Optional<PolicyProposal> findPolicyRequestById(PolicyProposalId id);

    PolicyProposal cancelPolicyRequest(PolicyProposalId id, String reason);
}
