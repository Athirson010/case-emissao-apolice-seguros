package io.github.athirson010.adapters.in.web.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Resposta de erro padrão da API")
public class ErrorResponse {

    @Schema(description = "Timestamp do erro", example = "2021-10-01T14:00:00Z")
    private String timestamp;

    @Schema(description = "Código de status HTTP", example = "400")
    private int status;

    @Schema(description = "Tipo do erro", example = "Validation Error")
    private String error;

    @Schema(description = "Mensagem descritiva do erro", example = "Erro(s) de validação nos campos da requisição")
    private String message;

    @Schema(description = "Mapa com os erros de validação por campo")
    private Map<String, String> errors;
}
