package io.github.athirson010.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@Getter
@EqualsAndHashCode
@ToString
public class PolicyRequestId {

    private final UUID value;

    private PolicyRequestId(UUID value) {
        this.value = value;
    }

    public static PolicyRequestId generate() {
        return new PolicyRequestId(UUID.randomUUID());
    }

    public static PolicyRequestId from(String id) {
        return new PolicyRequestId(UUID.fromString(id));
    }

    public static PolicyRequestId from(UUID id) {
        return new PolicyRequestId(id);
    }

    public String asString() {
        return value.toString();
    }
}
