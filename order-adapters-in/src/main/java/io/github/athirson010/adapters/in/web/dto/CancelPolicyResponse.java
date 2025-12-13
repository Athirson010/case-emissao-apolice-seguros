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
@Schema(description = "Resposta com os dados da proposta de apólice cancelada")
public class CancelPolicyResponse {

    @JsonProperty("policy_request_id")
    @Schema(
            description = "ID único da proposta de apólice cancelada",
            example = "89846cee-c6d5-4320-92e9-16e122d5c672"
    )
    private String policyRequestId;

    @JsonProperty("status")
    @Schema(
            description = "Status atual da proposta (sempre CANCELED após cancelamento)",
            example = "CANCELED",
            allowableValues = {"CANCELED"}
    )
    private String status;

    @JsonProperty("finished_at")
    @Schema(
            description = "Data/hora do cancelamento da proposta",
            example = "2021-10-01T15:30:00Z"
    )
    private String finishedAt;
}
