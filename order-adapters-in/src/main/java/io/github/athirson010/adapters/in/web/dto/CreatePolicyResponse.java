package io.github.athirson010.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePolicyResponse {

    @JsonProperty("policy_request_id")
    private String policyRequestId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("created_at")
    private String createdAt;
}
