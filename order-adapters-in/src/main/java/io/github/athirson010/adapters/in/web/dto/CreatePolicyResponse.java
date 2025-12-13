package io.github.athirson010.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta com os dados da proposta de apólice criada")
public class CreatePolicyResponse {

    @JsonProperty("policy_request_id")
    @Schema(
            description = "ID único da proposta de apólice",
            example = "89846cee-c6d5-4320-92e9-16e122d5c672"
    )
    private String policyRequestId;

    @JsonProperty("status")
    @Schema(
            description = "Status atual da proposta",
            example = "RECEIVED",
            allowableValues = {"RECEIVED", "VALIDATED", "PENDING", "APPROVED", "REJECTED", "CANCELED"}
    )
    private String status;

    @JsonProperty("created_at")
    @Schema(
            description = "Data/hora de criação da proposta",
            example = "2021-10-01T14:00:00Z"
    )
    private String createdAt;
}
