package io.github.athirson010.core.port.out;

import io.github.athirson010.domain.model.PolicyProposal;

public interface OrderEventPort {
    void sendOrderApprovedEvent(PolicyProposal policyProposal);
}
