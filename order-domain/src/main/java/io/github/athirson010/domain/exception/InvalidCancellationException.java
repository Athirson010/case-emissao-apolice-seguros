package io.github.athirson010.domain.exception;

import io.github.athirson010.domain.enums.PolicyStatus;

public class InvalidCancellationException extends DomainException {

    public InvalidCancellationException(String policyId, PolicyStatus currentStatus) {
        super(buildMessage(policyId, currentStatus));
    }

    private static String buildMessage(String policyId, PolicyStatus currentStatus) {
        if (PolicyStatus.CANCELED.equals(currentStatus)) {
            return String.format("A apólice %s já está cancelada. Não é possível cancelar novamente.", policyId);
        } else if (PolicyStatus.REJECTED.equals(currentStatus)) {
            return String.format("A apólice %s foi rejeitada. Não é possível cancelar uma apólice rejeitada.", policyId);
        } else {
            return String.format("A apólice %s está no status %s e não pode ser cancelada.", policyId, currentStatus);
        }
    }
}
