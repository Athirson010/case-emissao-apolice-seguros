package io.github.athirson010.adapters.in.web;

import io.github.athirson010.adapters.in.web.dto.CancelPolicyRequest;
import io.github.athirson010.adapters.in.web.dto.CreatePolicyRequest;
import io.github.athirson010.core.port.in.CreateOrderUseCase;
import io.github.athirson010.domain.enums.Category;
import io.github.athirson010.domain.enums.PaymentMethod;
import io.github.athirson010.domain.enums.PolicyStatus;
import io.github.athirson010.domain.exception.InvalidCancellationException;
import io.github.athirson010.domain.model.Money;
import io.github.athirson010.domain.model.PolicyProposal;
import io.github.athirson010.domain.model.PolicyProposalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PolicyRequestController - Testes Unitários")
class PolicyRequestControllerTest {

    @Mock
    private CreateOrderUseCase createOrderUseCase;

    @InjectMocks
    private PolicyRequestController policyRequestController;

    private CreatePolicyRequest createRequest;
    private PolicyProposal policyProposal;
    private PolicyProposalId policyId;

    @BeforeEach
    void setUp() {
        createRequest = CreatePolicyRequest.builder()
                .customerId(UUID.randomUUID().toString())
                .productId("PROD-AUTO-2024")
                .category("AUTO")
                .salesChannel("MOBILE")
                .paymentMethod("CREDIT_CARD")
                .totalMonthlyPremiumAmount("350.00")
                .insuredAmount("200000.00")
                .coverages(Map.of("COLISAO", "200000.00"))
                .assistances(List.of("GUINCHO_24H"))
                .build();

        policyId = PolicyProposalId.generate();

        policyProposal = PolicyProposal.create(
                UUID.randomUUID(),
                "PROD-AUTO-2024",
                Category.AUTO,
                io.github.athirson010.domain.enums.SalesChannel.MOBILE,
                PaymentMethod.CREDIT_CARD,
                Money.brl(new BigDecimal("350.00")),
                Money.brl(new BigDecimal("200000.00")),
                Map.of("COLISAO", Money.brl(new BigDecimal("200000.00"))),
                List.of("GUINCHO_24H"),
                java.time.Instant.now()
        );
    }

    @Test
    @DisplayName("Deve criar proposta de apólice com sucesso")
    void shouldCreatePolicySuccessfully() {
        // Given
        when(createOrderUseCase.createPolicyRequest(any(PolicyProposal.class)))
                .thenReturn(policyProposal);

        // When
        ResponseEntity<?> response = policyRequestController.createPolicy(createRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();

        verify(createOrderUseCase, times(1)).createPolicyRequest(any(PolicyProposal.class));
    }

    @Test
    @DisplayName("Deve cancelar proposta com sucesso")
    void shouldCancelPolicySuccessfully() {
        // Given
        CancelPolicyRequest cancelRequest = CancelPolicyRequest.builder()
                .reason("Cliente solicitou cancelamento")
                .build();

        policyProposal.cancel("Cliente solicitou cancelamento", java.time.Instant.now());

        when(createOrderUseCase.cancelPolicyRequest(any(PolicyProposalId.class), eq("Cliente solicitou cancelamento")))
                .thenReturn(policyProposal);

        // When
        ResponseEntity<?> response = policyRequestController.cancelPolicy(
                policyId.asString(),
                cancelRequest
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        verify(createOrderUseCase, times(1))
                .cancelPolicyRequest(any(PolicyProposalId.class), eq("Cliente solicitou cancelamento"));
    }

    @Test
    @DisplayName("Deve retornar 404 quando proposta não for encontrada ao cancelar")
    void shouldReturn404WhenPolicyNotFoundOnCancel() {
        // Given
        CancelPolicyRequest cancelRequest = CancelPolicyRequest.builder()
                .reason("Cliente solicitou cancelamento")
                .build();

        when(createOrderUseCase.cancelPolicyRequest(any(PolicyProposalId.class), anyString()))
                .thenThrow(new IllegalArgumentException("Proposta não encontrada"));

        // When
        ResponseEntity<?> response = policyRequestController.cancelPolicy(
                policyId.asString(),
                cancelRequest
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        verify(createOrderUseCase, times(1))
                .cancelPolicyRequest(any(PolicyProposalId.class), anyString());
    }

    @Test
    @DisplayName("Deve buscar proposta por ID com sucesso")
    void shouldGetPolicyByIdSuccessfully() {
        // Given
        when(createOrderUseCase.findPolicyRequestById(any(PolicyProposalId.class)))
                .thenReturn(Optional.of(policyProposal));

        // When
        ResponseEntity<?> response = policyRequestController.getPolicy(policyId.asString());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        verify(createOrderUseCase, times(1)).findPolicyRequestById(any(PolicyProposalId.class));
    }

    @Test
    @DisplayName("Deve retornar 404 quando proposta não for encontrada ao buscar")
    void shouldReturn404WhenPolicyNotFoundOnGet() {
        // Given
        when(createOrderUseCase.findPolicyRequestById(any(PolicyProposalId.class)))
                .thenReturn(Optional.empty());

        // When
        ResponseEntity<?> response = policyRequestController.getPolicy(policyId.asString());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        verify(createOrderUseCase, times(1)).findPolicyRequestById(any(PolicyProposalId.class));
    }
}
