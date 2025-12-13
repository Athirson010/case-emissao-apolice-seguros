package io.github.athirson010.adapters.in.web;

import io.github.athirson010.adapters.in.web.dto.CancelPolicyRequest;
import io.github.athirson010.adapters.in.web.dto.CancelPolicyResponse;
import io.github.athirson010.adapters.in.web.dto.CreatePolicyRequest;
import io.github.athirson010.adapters.in.web.dto.CreatePolicyResponse;
import io.github.athirson010.adapters.in.web.mapper.PolicyRequestMapper;
import io.github.athirson010.core.port.in.CreateOrderUseCase;
import io.github.athirson010.domain.model.PolicyProposal;
import io.github.athirson010.domain.model.PolicyProposalId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/policies")
@RequiredArgsConstructor
@Tag(name = "Policy Proposals", description = "Endpoints para gerenciamento de propostas de apólices de seguros")
public class PolicyRequestController {

    private final CreateOrderUseCase createOrderUseCase;

    @PostMapping
    @Operation(
            summary = "Criar nova proposta de apólice",
            description = "Cria uma nova proposta de apólice de seguro com os dados fornecidos. A proposta é criada no estado RECEIVED."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Proposta criada com sucesso",
                    content = @Content(schema = @Schema(implementation = CreatePolicyResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Dados inválidos fornecidos")
    })
    public ResponseEntity<CreatePolicyResponse> createPolicy(
            @Parameter(description = "Dados da proposta de apólice a ser criada", required = true)
            @RequestBody @jakarta.validation.Valid CreatePolicyRequest request) {
        log.info("Received request to create policy for customer: {}", request.getCustomerId());

        PolicyProposal policyProposal = PolicyRequestMapper.toDomain(request);
        PolicyProposal savedPolicy = createOrderUseCase.createPolicyRequest(policyProposal);

        log.info("Policy proposal created and persisted with ID: {}", savedPolicy.getId().asString());

        CreatePolicyResponse response = PolicyRequestMapper.toCreateResponse(savedPolicy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/cancel")
    @Operation(
            summary = "Cancelar proposta de apólice",
            description = "Cancela uma proposta de apólice existente. Apenas propostas que não estão em estado final podem ser canceladas."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Proposta cancelada com sucesso",
                    content = @Content(schema = @Schema(implementation = CancelPolicyResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Proposta não encontrada"),
            @ApiResponse(responseCode = "400", description = "Proposta já está em estado final e não pode ser cancelada")
    })
    public ResponseEntity<CancelPolicyResponse> cancelPolicy(
            @Parameter(description = "ID da proposta a ser cancelada", required = true, example = "8a5c3e1b-9f2d-4a7e-b3c8-1d4e5f6a7b8c")
            @PathVariable String id,
            @Parameter(description = "Motivo do cancelamento", required = true)
            @RequestBody @jakarta.validation.Valid CancelPolicyRequest request
    ) {
        log.info("Received request to cancel policy: {}", id);

        try {
            PolicyProposalId policyId = PolicyProposalId.from(id);
            PolicyProposal cancelledPolicy = createOrderUseCase.cancelPolicyRequest(policyId, request.getReason());

            log.info("Policy request cancelled and persisted: {}", id);

            CancelPolicyResponse response = PolicyRequestMapper.toCancelResponse(cancelledPolicy);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Policy request not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Consultar proposta de apólice",
            description = "Busca uma proposta de apólice específica pelo seu ID"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Proposta encontrada",
                    content = @Content(schema = @Schema(implementation = CreatePolicyResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Proposta não encontrada")
    })
    public ResponseEntity<CreatePolicyResponse> getPolicy(
            @Parameter(description = "ID da proposta", required = true, example = "8a5c3e1b-9f2d-4a7e-b3c8-1d4e5f6a7b8c")
            @PathVariable String id) {
        log.info("Received request to retrieve policy: {}", id);

        PolicyProposalId policyId = PolicyProposalId.from(id);

        return createOrderUseCase.findPolicyRequestById(policyId)
                .map(policyProposal -> {
                    CreatePolicyResponse response = CreatePolicyResponse.builder()
                            .policyRequestId(policyProposal.getId().asString())
                            .status(policyProposal.getStatus().name())
                            .createdAt(policyProposal.getCreatedAt().toString())
                            .build();
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    log.warn("Policy proposal not found: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }
}
