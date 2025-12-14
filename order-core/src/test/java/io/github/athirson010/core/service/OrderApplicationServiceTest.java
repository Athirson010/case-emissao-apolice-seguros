package io.github.athirson010.core.service;

import io.github.athirson010.core.port.out.FraudQueuePort;
import io.github.athirson010.core.port.out.OrderRepository;
import io.github.athirson010.domain.enums.Category;
import io.github.athirson010.domain.enums.PaymentMethod;
import io.github.athirson010.domain.enums.PolicyStatus;
import io.github.athirson010.domain.enums.SalesChannel;
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

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderApplicationService - Testes Unitários")
class OrderApplicationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private FraudQueuePort fraudQueuePort;

    @InjectMocks
    private OrderApplicationService orderApplicationService;

    private PolicyProposal policyProposal;
    private PolicyProposalId policyId;

    @BeforeEach
    void setUp() {
        policyProposal = PolicyProposal.create(
                UUID.randomUUID(),
                "PROD-AUTO-2024",
                Category.AUTO,
                SalesChannel.MOBILE,
                PaymentMethod.CREDIT_CARD,
                Money.brl(new BigDecimal("350.00")),
                Money.brl(new BigDecimal("200000.00")),
                Map.of("COLISAO", Money.brl(new BigDecimal("200000.00"))),
                List.of("GUINCHO_24H"),
                java.time.Instant.now()
        );

        policyId = policyProposal.getId();
    }

    @Test
    @DisplayName("Deve criar proposta de apólice com sucesso")
    void shouldCreatePolicyRequestSuccessfully() {
        // Given
        when(orderRepository.save(any(PolicyProposal.class))).thenReturn(policyProposal);

        // When
        PolicyProposal result = orderApplicationService.createPolicyRequest(policyProposal);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(PolicyStatus.RECEIVED);

        verify(orderRepository, times(1)).save(policyProposal);
        verify(fraudQueuePort, times(1)).sendToFraudQueue(policyProposal);
    }

    @Test
    @DisplayName("Deve buscar proposta por ID com sucesso")
    void shouldFindPolicyRequestById() {
        // Given
        when(orderRepository.findById(policyId)).thenReturn(Optional.of(policyProposal));

        // When
        Optional<PolicyProposal> result = orderApplicationService.findPolicyRequestById(policyId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(policyId);

        verify(orderRepository, times(1)).findById(policyId);
    }

    @Test
    @DisplayName("Deve retornar vazio quando proposta não existir")
    void shouldReturnEmptyWhenPolicyNotFound() {
        // Given
        when(orderRepository.findById(policyId)).thenReturn(Optional.empty());

        // When
        Optional<PolicyProposal> result = orderApplicationService.findPolicyRequestById(policyId);

        // Then
        assertThat(result).isEmpty();

        verify(orderRepository, times(1)).findById(policyId);
    }

    @Test
    @DisplayName("Deve cancelar proposta com status RECEIVED com sucesso")
    void shouldCancelPolicyWithReceivedStatus() {
        // Given
        when(orderRepository.findById(policyId)).thenReturn(Optional.of(policyProposal));
        when(orderRepository.save(any(PolicyProposal.class))).thenReturn(policyProposal);

        // When
        PolicyProposal result = orderApplicationService.cancelPolicyRequest(policyId, "Cliente solicitou cancelamento");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(PolicyStatus.CANCELED);

        verify(orderRepository, times(1)).findById(policyId);
        verify(orderRepository, times(1)).save(any(PolicyProposal.class));
        verify(fraudQueuePort, times(1)).sendToFraudQueue(any(PolicyProposal.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar cancelar proposta já cancelada")
    void shouldThrowExceptionWhenCancellingAlreadyCancelledPolicy() {
        // Given
        policyProposal.cancel("Motivo anterior", java.time.Instant.now());
        when(orderRepository.findById(policyId)).thenReturn(Optional.of(policyProposal));

        // When/Then
        assertThatThrownBy(() -> orderApplicationService.cancelPolicyRequest(policyId, "Novo motivo"))
                .isInstanceOf(InvalidCancellationException.class)
                .hasMessageContaining("já está cancelada");

        verify(orderRepository, times(1)).findById(policyId);
        verify(orderRepository, never()).save(any(PolicyProposal.class));
        verify(fraudQueuePort, never()).sendToFraudQueue(any(PolicyProposal.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar cancelar proposta rejeitada")
    void shouldThrowExceptionWhenCancellingRejectedPolicy() {
        // Given
        policyProposal.validate(java.time.Instant.now());
        policyProposal.reject("Motivo de rejeição", java.time.Instant.now());
        when(orderRepository.findById(policyId)).thenReturn(Optional.of(policyProposal));

        // When/Then
        assertThatThrownBy(() -> orderApplicationService.cancelPolicyRequest(policyId, "Motivo de cancelamento"))
                .isInstanceOf(InvalidCancellationException.class)
                .hasMessageContaining("foi rejeitada");

        verify(orderRepository, times(1)).findById(policyId);
        verify(orderRepository, never()).save(any(PolicyProposal.class));
        verify(fraudQueuePort, never()).sendToFraudQueue(any(PolicyProposal.class));
    }

    @Test
    @DisplayName("Deve permitir cancelar proposta aprovada")
    void shouldAllowCancellingApprovedPolicy() {
        // Given
        policyProposal.validate(java.time.Instant.now());
        policyProposal.approve(java.time.Instant.now());
        when(orderRepository.findById(policyId)).thenReturn(Optional.of(policyProposal));
        when(orderRepository.save(any(PolicyProposal.class))).thenReturn(policyProposal);

        // When
        PolicyProposal result = orderApplicationService.cancelPolicyRequest(policyId, "Cliente solicitou cancelamento");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(PolicyStatus.CANCELED);

        verify(orderRepository, times(1)).findById(policyId);
        verify(orderRepository, times(1)).save(any(PolicyProposal.class));
        verify(fraudQueuePort, times(1)).sendToFraudQueue(any(PolicyProposal.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao cancelar proposta não encontrada")
    void shouldThrowExceptionWhenCancellingNonExistentPolicy() {
        // Given
        when(orderRepository.findById(policyId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> orderApplicationService.cancelPolicyRequest(policyId, "Motivo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não encontrada");

        verify(orderRepository, times(1)).findById(policyId);
        verify(orderRepository, never()).save(any(PolicyProposal.class));
    }

    @Test
    @DisplayName("Deve buscar proposta por ID do cliente")
    void shouldFindPolicyByCustomerId() {
        // Given
        UUID customerId = UUID.randomUUID();
        when(orderRepository.findByCustomerId(customerId)).thenReturn(Optional.of(policyProposal));

        // When
        Optional<PolicyProposal> result = orderApplicationService.findPolicyRequestByCustomerId(customerId);

        // Then
        assertThat(result).isPresent();

        verify(orderRepository, times(1)).findByCustomerId(customerId);
    }
}
