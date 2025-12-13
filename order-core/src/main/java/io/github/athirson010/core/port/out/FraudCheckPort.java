package io.github.athirson010.core.port.out;

import io.github.athirson010.domain.model.FraudAnalysisResult;
import io.github.athirson010.domain.model.PolicyProposal;

public interface FraudCheckPort {

    FraudAnalysisResult analyzeFraud(PolicyProposal policyProposal);
}
