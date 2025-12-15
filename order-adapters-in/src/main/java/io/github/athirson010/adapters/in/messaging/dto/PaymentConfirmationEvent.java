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
public class PaymentConfirmationEvent {

    @JsonProperty("policy_request_id")
    private String policyRequestId;

    @JsonProperty("payment_status")
    private String paymentStatus;

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("payment_timestamp")
    private Instant paymentTimestamp;

    @JsonProperty("rejection_reason")
    private String rejectionReason;

    public boolean isApproved() {
        return "APPROVED".equalsIgnoreCase(paymentStatus);
    }

    public boolean isRejected() {
        return "REJECTED".equalsIgnoreCase(paymentStatus);
    }
}
