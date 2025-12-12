package io.github.athirson010.adapters.in.web;

import io.github.athirson010.adapters.in.web.dto.CancelPolicyRequest;
import io.github.athirson010.adapters.in.web.dto.CancelPolicyResponse;
import io.github.athirson010.adapters.in.web.dto.CreatePolicyRequest;
import io.github.athirson010.adapters.in.web.dto.CreatePolicyResponse;
import io.github.athirson010.adapters.in.web.mapper.PolicyRequestMapper;
import io.github.athirson010.application.port.in.CreateOrderUseCase;
import io.github.athirson010.domain.model.PolicyRequest;
import io.github.athirson010.domain.model.PolicyRequestId;
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

        PolicyRequest policyRequest = PolicyRequestMapper.toDomain(request);
        PolicyRequest savedPolicy = createOrderUseCase.createPolicyRequest(policyRequest);

        log.info("Policy request created and persisted with ID: {}", savedPolicy.getId().asString());

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
            PolicyRequestId policyId = PolicyRequestId.from(id);
            PolicyRequest cancelledPolicy = createOrderUseCase.cancelPolicyRequest(policyId, request.getReason());

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

        PolicyRequestId policyId = PolicyRequestId.from(id);

        return createOrderUseCase.findPolicyRequestById(policyId)
                .map(policyRequest -> {
                    CreatePolicyResponse response = CreatePolicyResponse.builder()
                            .policyRequestId(policyRequest.getId().asString())
                            .status(policyRequest.getStatus().name())
                            .createdAt(policyRequest.getCreatedAt().toString())
                            .build();
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    log.warn("Policy request not found: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }
}
