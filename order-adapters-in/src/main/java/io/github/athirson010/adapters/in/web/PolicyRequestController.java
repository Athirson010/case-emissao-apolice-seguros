package io.github.athirson010.adapters.in.web;

import io.github.athirson010.adapters.in.web.dto.CancelPolicyRequest;
import io.github.athirson010.adapters.in.web.dto.CancelPolicyResponse;
import io.github.athirson010.adapters.in.web.dto.CreatePolicyRequest;
import io.github.athirson010.adapters.in.web.dto.CreatePolicyResponse;
import io.github.athirson010.adapters.in.web.mapper.PolicyRequestMapper;
import io.github.athirson010.core.port.in.CreateOrderUseCase;
import io.github.athirson010.domain.model.PolicyProposal;
import io.github.athirson010.domain.model.PolicyProposalId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/policies")
@RequiredArgsConstructor
public class PolicyRequestController {

    private final CreateOrderUseCase createOrderUseCase;

    @PostMapping
    public ResponseEntity<CreatePolicyResponse> createPolicy(@RequestBody CreatePolicyRequest request) {
        log.info("Received request to create policy for customer: {}", request.getCustomerId());

        PolicyProposal policyProposal = PolicyRequestMapper.toDomain(request);
        PolicyProposal savedPolicy = createOrderUseCase.createPolicyRequest(policyProposal);

        log.info("Policy proposal created and persisted with ID: {}", savedPolicy.getId().asString());

        CreatePolicyResponse response = PolicyRequestMapper.toCreateResponse(savedPolicy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<CancelPolicyResponse> cancelPolicy(
            @PathVariable String id,
            @RequestBody CancelPolicyRequest request
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
    public ResponseEntity<CreatePolicyResponse> getPolicy(@PathVariable String id) {
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
