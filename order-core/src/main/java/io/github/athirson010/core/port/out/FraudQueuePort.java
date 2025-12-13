package io.github.athirson010.core.port.out;

import io.github.athirson010.domain.model.PolicyProposal;

public interface FraudQueuePort {

    void sendToFraudQueue(PolicyProposal policyProposal);
}
