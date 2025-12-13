package io.github.athirson010.core.port.in;

import io.github.athirson010.domain.model.PolicyProposal;
import io.github.athirson010.domain.model.PolicyProposalId;

import java.util.Optional;
import java.util.UUID;

public interface CreateOrderUseCase {

    PolicyProposal createPolicyRequest(PolicyProposal policyProposal);

    Optional<PolicyProposal> findPolicyRequestById(PolicyProposalId id);

    Optional<PolicyProposal> findPolicyRequestByCustomerId(UUID customerId);

    PolicyProposal cancelPolicyRequest(PolicyProposalId id, String reason);
}
