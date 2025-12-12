package io.github.athirson010.domain.exception;

import io.github.athirson010.domain.enums.PolicyStatus;

public class InvalidTransitionException extends DomainException {

    public InvalidTransitionException(PolicyStatus from, PolicyStatus to) {
        super(String.format("Invalid transition from %s to %s", from, to));
    }

    public InvalidTransitionException(String message) {
        super(message);
    }
}
