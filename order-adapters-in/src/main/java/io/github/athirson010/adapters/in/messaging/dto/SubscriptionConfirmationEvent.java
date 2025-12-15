package io.github.athirson010.adapters.in.messaging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionConfirmationEvent {

    @JsonProperty("policy_request_id")
    private String policyRequestId;

    @JsonProperty("subscription_status")
    private String subscriptionStatus;

    @JsonProperty("subscription_id")
    private String subscriptionId;

    @JsonProperty("authorization_timestamp")
    private Instant authorizationTimestamp;

    @JsonProperty("rejection_reason")
    private String rejectionReason;

    public boolean isApproved() {
        return "APPROVED".equalsIgnoreCase(subscriptionStatus);
    }

    public boolean isRejected() {
        return "REJECTED".equalsIgnoreCase(subscriptionStatus);
    }
}
