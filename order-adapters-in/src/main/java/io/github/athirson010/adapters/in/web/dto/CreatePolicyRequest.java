package io.github.athirson010.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.athirson010.adapters.in.web.validation.annotation.ValidCoverages;
import io.github.athirson010.adapters.in.web.validation.annotation.ValidEnum;
import io.github.athirson010.adapters.in.web.validation.annotation.ValidMoneyAmount;
import io.github.athirson010.adapters.in.web.validation.annotation.ValidUUID;
import io.github.athirson010.domain.enums.Category;
import io.github.athirson010.domain.enums.PaymentMethod;
import io.github.athirson010.domain.enums.SalesChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados para criação de uma nova proposta de apólice de seguro")
public class CreatePolicyRequest {

    @NotBlank(message = "ID do cliente é obrigatório")
    @ValidUUID(message = "ID do cliente deve ser um UUID válido")
    @JsonProperty("customer_id")
    @Schema(
            description = "ID do cliente em formato UUID",
            example = "adc56d77-348c-4bf0-908f-22d402ee715c",
            required = true
    )
    private String customerId;

    @NotBlank(message = "ID do produto é obrigatório")
    @JsonProperty("product_id")
    @Schema(
            description = "ID do produto de seguro",
            example = "1b2da7cc-b367-4196-8a78-9cfeec21f587",
            required = true
    )
    private String productId;

    @NotBlank(message = "Categoria é obrigatória")
    @ValidEnum(enumClass = Category.class, message = "Categoria inválida. Valores aceitos: AUTO, VIDA, RESIDENCIAL, EMPRESARIAL, OUTROS")
    @JsonProperty("category")
    @Schema(
            description = "Categoria do seguro",
            example = "AUTO",
            allowableValues = {"AUTO", "VIDA", "RESIDENCIAL", "EMPRESARIAL", "OUTROS"},
            required = true
    )
    private String category;

    @NotBlank(message = "Canal de vendas é obrigatório")
    @ValidEnum(enumClass = SalesChannel.class, message = "Canal de vendas inválido. Valores aceitos: MOBILE, WEB, WHATSAPP, OUTROS")
    @JsonProperty("sales_channel")
    @Schema(
            description = "Canal de vendas da solicitação",
            example = "MOBILE",
            allowableValues = {"MOBILE", "WEB", "WHATSAPP", "OUTROS"},
            required = true
    )
    private String salesChannel;

    @NotBlank(message = "Forma de pagamento é obrigatória")
    @ValidEnum(enumClass = PaymentMethod.class, message = "Forma de pagamento inválida. Valores aceitos: CREDIT_CARD, DEBIT, BOLETO, PIX")
    @JsonProperty("payment_method")
    @Schema(
            description = "Forma de pagamento da solicitação",
            example = "CREDIT_CARD",
            allowableValues = {"CREDIT_CARD", "DEBIT", "BOLETO", "PIX"},
            required = true
    )
    private String paymentMethod;

    @NotBlank(message = "Valor do prêmio mensal é obrigatório")
    @ValidMoneyAmount(message = "Valor do prêmio mensal deve ser um número positivo válido")
    @JsonProperty("total_monthly_premium_amount")
    @Schema(
            description = "Valor total do prêmio mensal (valor que o segurado paga mensalmente)",
            example = "75.25",
            required = true
    )
    private String totalMonthlyPremiumAmount;

    @NotBlank(message = "Valor segurado é obrigatório")
    @ValidMoneyAmount(message = "Valor segurado deve ser um número positivo válido")
    @JsonProperty("insured_amount")
    @Schema(
            description = "Valor do capital segurado (valor máximo que a seguradora pagará)",
            example = "275000.50",
            required = true
    )
    private String insuredAmount;

    @NotNull(message = "Coberturas são obrigatórias")
    @NotEmpty(message = "Deve haver pelo menos uma cobertura")
    @ValidCoverages(message = "Coberturas contêm valores inválidos")
    @JsonProperty("coverages")
    @Schema(
            description = "Lista de coberturas da solicitação de apólice (nome da cobertura e valor)",
            example = "{\"Roubo\": \"100000.25\", \"Perda Total\": \"100000.25\", \"Colisão com Terceiros\": \"75000.00\"}",
            required = true
    )
    private Map<String, String> coverages;

    @NotNull(message = "Assistências são obrigatórias")
    @NotEmpty(message = "Deve haver pelo menos uma assistência")
    @JsonProperty("assistances")
    @Schema(
            description = "Lista de assistências incluídas na apólice",
            example = "[\"Guincho até 250km\", \"Troca de Óleo\", \"Chaveiro 24h\"]",
            required = true
    )
    private List<String> assistances;
}
