package io.github.athirson010.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados para cancelamento de uma proposta de apólice")
public class CancelPolicyRequest {

    @NotBlank(message = "Motivo do cancelamento é obrigatório")
    @JsonProperty("reason")
    @Schema(
            description = "Motivo do cancelamento da proposta",
            example = "Cliente desistiu da contratação",
            required = true
    )
    private String reason;
}
