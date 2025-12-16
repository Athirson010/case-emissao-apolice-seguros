package io.github.athirson010.adapters.in.messaging.rabbitmq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.athirson010.adapters.in.messaging.dto.PaymentConfirmationEvent;
import io.github.athirson010.core.port.out.OrderRepository;
import io.github.athirson010.domain.enums.Category;
import io.github.athirson010.domain.enums.PaymentMethod;
import io.github.athirson010.domain.enums.PolicyStatus;
import io.github.athirson010.domain.enums.SalesChannel;
import io.github.athirson010.domain.model.Money;
import io.github.athirson010.domain.model.PolicyProposal;
import io.github.athirson010.domain.model.PolicyProposalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentConfirmationConsumer - Testes Unitários")
class PaymentConfirmationConsumerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentConfirmationConsumer consumer;

    private String validPolicyId;
    private PolicyProposal policyProposal;
    private String validMessageBody;

    @BeforeEach
    void setUp() {
        validPolicyId = UUID.randomUUID().toString();

        // Cria uma proposta no estado PENDING
        policyProposal = PolicyProposal.builder()
                .id(PolicyProposalId.from(validPolicyId))
                .customerId(UUID.randomUUID())
                .productId("PROD-AUTO-2024")
                .category(Category.AUTO)
                .salesChannel(SalesChannel.MOBILE)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .totalMonthlyPremiumAmount(Money.brl(BigDecimal.valueOf(350.00)))
                .insuredAmount(Money.brl(BigDecimal.valueOf(200000.00)))
                .coverages(Map.of("COLISAO", Money.brl(BigDecimal.valueOf(200000.00))))
                .assistances(List.of("GUINCHO_24H"))
                .status(PolicyStatus.PENDING)
                .createdAt(Instant.now())
                .subscriptionResponseReceived(true)
                .subscriptionConfirmed(true)
                .build();

        validMessageBody = "{\"policy_request_id\":\"" + validPolicyId + "\",\"payment_status\":\"APPROVED\"}";
    }

    @Test
    @DisplayName("Deve processar confirmação de pagamento aprovado com sucesso")
    void deveProcessarConfirmacaoDePagamentoAprovadoComSucesso() throws Exception {
        // Given
        PaymentConfirmationEvent event = PaymentConfirmationEvent.builder()
                .policyRequestId(validPolicyId)
                .paymentStatus("APPROVED")
                .transactionId("TXN-12345")
                .amount("350.00")
                .paymentMethod("CREDIT_CARD")
                .paymentTimestamp(Instant.now())
                .build();

        when(objectMapper.readValue(validMessageBody, PaymentConfirmationEvent.class))
                .thenReturn(event);
        when(orderRepository.findById(any(PolicyProposalId.class)))
                .thenReturn(Optional.of(policyProposal));
        when(orderRepository.save(any(PolicyProposal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        consumer.consumePaymentConfirmation(validMessageBody);

        // Then
        ArgumentCaptor<PolicyProposal> captor = ArgumentCaptor.forClass(PolicyProposal.class);
        verify(orderRepository).save(captor.capture());

        PolicyProposal savedProposal = captor.getValue();
        assertThat(savedProposal.getStatus()).isEqualTo(PolicyStatus.APPROVED);
        assertThat(savedProposal.isPaymentResponseReceived()).isTrue();
        assertThat(savedProposal.isPaymentConfirmed()).isTrue();
        assertThat(savedProposal.getFinishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve processar confirmação de pagamento rejeitado com sucesso")
    void deveProcessarConfirmacaoDePagamentoRejeitadoComSucesso() throws Exception {
        // Given
        String rejectionReason = "Cartão de crédito recusado";
        PaymentConfirmationEvent event = PaymentConfirmationEvent.builder()
                .policyRequestId(validPolicyId)
                .paymentStatus("REJECTED")
                .transactionId("TXN-12345")
                .amount("350.00")
                .paymentMethod("CREDIT_CARD")
                .paymentTimestamp(Instant.now())
                .rejectionReason(rejectionReason)
                .build();

        String messageBody = "{\"policy_request_id\":\"" + validPolicyId + "\",\"payment_status\":\"REJECTED\"}";

        when(objectMapper.readValue(messageBody, PaymentConfirmationEvent.class))
                .thenReturn(event);
        when(orderRepository.findById(any(PolicyProposalId.class)))
                .thenReturn(Optional.of(policyProposal));
        when(orderRepository.save(any(PolicyProposal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        consumer.consumePaymentConfirmation(messageBody);

        // Then
        ArgumentCaptor<PolicyProposal> captor = ArgumentCaptor.forClass(PolicyProposal.class);
        verify(orderRepository).save(captor.capture());

        PolicyProposal savedProposal = captor.getValue();
        assertThat(savedProposal.getStatus()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(savedProposal.isPaymentResponseReceived()).isTrue();
        assertThat(savedProposal.isPaymentConfirmed()).isFalse();
        assertThat(savedProposal.getPaymentRejectionReason()).isEqualTo(rejectionReason);
        assertThat(savedProposal.getFinishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve lançar exceção quando proposta de apólice não for encontrada")
    void deveLancarExcecaoQuandoPropostaDeApoliceNaoForEncontrada() throws Exception {
        // Given
        PaymentConfirmationEvent event = PaymentConfirmationEvent.builder()
                .policyRequestId(validPolicyId)
                .paymentStatus("APPROVED")
                .transactionId("TXN-12345")
                .build();

        when(objectMapper.readValue(validMessageBody, PaymentConfirmationEvent.class))
                .thenReturn(event);
        when(orderRepository.findById(any(PolicyProposalId.class)))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> consumer.consumePaymentConfirmation(validMessageBody))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha ao processar mensagem de confirmação de pagamento")
                .hasCauseInstanceOf(IllegalArgumentException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando houver erro de desserialização")
    void deveLancarExcecaoQuandoHouverErroDeDesserializacao() throws Exception {
        // Given
        String invalidMessageBody = "invalid json";

        when(objectMapper.readValue(invalidMessageBody, PaymentConfirmationEvent.class))
                .thenThrow(new JsonProcessingException("Invalid JSON") {});

        // When & Then
        assertThatThrownBy(() -> consumer.consumePaymentConfirmation(invalidMessageBody))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha ao processar mensagem de confirmação de pagamento")
                .hasCauseInstanceOf(JsonProcessingException.class);

        verify(orderRepository, never()).findById(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve processar pagamento aprovado mesmo quando subscrição ainda não foi respondida")
    void deveProcessarPagamentoAprovadoMesmoQuandoSubscricaoAindaNaoFoiRespondida() throws Exception {
        // Given
        policyProposal = PolicyProposal.builder()
                .id(PolicyProposalId.from(validPolicyId))
                .customerId(UUID.randomUUID())
                .productId("PROD-AUTO-2024")
                .category(Category.AUTO)
                .salesChannel(SalesChannel.MOBILE)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .totalMonthlyPremiumAmount(Money.brl(BigDecimal.valueOf(350.00)))
                .insuredAmount(Money.brl(BigDecimal.valueOf(200000.00)))
                .coverages(Map.of("COLISAO", Money.brl(BigDecimal.valueOf(200000.00))))
                .assistances(List.of("GUINCHO_24H"))
                .status(PolicyStatus.PENDING)
                .createdAt(Instant.now())
                .subscriptionResponseReceived(false)  // Subscrição ainda não respondeu
                .subscriptionConfirmed(false)
                .build();

        PaymentConfirmationEvent event = PaymentConfirmationEvent.builder()
                .policyRequestId(validPolicyId)
                .paymentStatus("APPROVED")
                .transactionId("TXN-12345")
                .build();

        when(objectMapper.readValue(validMessageBody, PaymentConfirmationEvent.class))
                .thenReturn(event);
        when(orderRepository.findById(any(PolicyProposalId.class)))
                .thenReturn(Optional.of(policyProposal));
        when(orderRepository.save(any(PolicyProposal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        consumer.consumePaymentConfirmation(validMessageBody);

        // Then
        ArgumentCaptor<PolicyProposal> captor = ArgumentCaptor.forClass(PolicyProposal.class);
        verify(orderRepository).save(captor.capture());

        PolicyProposal savedProposal = captor.getValue();
        // Deve permanecer PENDING aguardando resposta de subscrição
        assertThat(savedProposal.getStatus()).isEqualTo(PolicyStatus.PENDING);
        assertThat(savedProposal.isPaymentResponseReceived()).isTrue();
        assertThat(savedProposal.isPaymentConfirmed()).isTrue();
        assertThat(savedProposal.getFinishedAt()).isNull();
    }

    @Test
    @DisplayName("Deve processar pagamento após policy já ter sido rejeitada por subscrição")
    void deveProcessarPagamentoAposPoliceJaTerSidoRejeitadaPorSubscricao() throws Exception {
        // Given - Policy já foi rejeitada por subscrição
        policyProposal = PolicyProposal.builder()
                .id(PolicyProposalId.from(validPolicyId))
                .customerId(UUID.randomUUID())
                .productId("PROD-AUTO-2024")
                .category(Category.AUTO)
                .salesChannel(SalesChannel.MOBILE)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .totalMonthlyPremiumAmount(Money.brl(BigDecimal.valueOf(350.00)))
                .insuredAmount(Money.brl(BigDecimal.valueOf(200000.00)))
                .coverages(Map.of("COLISAO", Money.brl(BigDecimal.valueOf(200000.00))))
                .assistances(List.of("GUINCHO_24H"))
                .status(PolicyStatus.REJECTED)  // Já foi rejeitada
                .createdAt(Instant.now())
                .finishedAt(Instant.now())
                .subscriptionResponseReceived(true)
                .subscriptionConfirmed(false)
                .subscriptionRejectionReason("Perfil de risco não aceito")
                .build();

        PaymentConfirmationEvent event = PaymentConfirmationEvent.builder()
                .policyRequestId(validPolicyId)
                .paymentStatus("APPROVED")
                .transactionId("TXN-12345")
                .build();

        when(objectMapper.readValue(validMessageBody, PaymentConfirmationEvent.class))
                .thenReturn(event);
        when(orderRepository.findById(any(PolicyProposalId.class)))
                .thenReturn(Optional.of(policyProposal));
        when(orderRepository.save(any(PolicyProposal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        consumer.consumePaymentConfirmation(validMessageBody);

        // Then
        ArgumentCaptor<PolicyProposal> captor = ArgumentCaptor.forClass(PolicyProposal.class);
        verify(orderRepository).save(captor.capture());

        PolicyProposal savedProposal = captor.getValue();
        // Deve permanecer REJECTED
        assertThat(savedProposal.getStatus()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(savedProposal.isPaymentResponseReceived()).isTrue();
        assertThat(savedProposal.isPaymentConfirmed()).isTrue();
    }

    @Test
    @DisplayName("Deve lançar exceção quando resposta de pagamento já foi recebida")
    void deveLancarExcecaoQuandoRespostaDePagamentoJaFoiRecebida() throws Exception {
        // Given
        policyProposal = PolicyProposal.builder()
                .id(PolicyProposalId.from(validPolicyId))
                .customerId(UUID.randomUUID())
                .productId("PROD-AUTO-2024")
                .category(Category.AUTO)
                .salesChannel(SalesChannel.MOBILE)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .totalMonthlyPremiumAmount(Money.brl(BigDecimal.valueOf(350.00)))
                .insuredAmount(Money.brl(BigDecimal.valueOf(200000.00)))
                .coverages(Map.of("COLISAO", Money.brl(BigDecimal.valueOf(200000.00))))
                .assistances(List.of("GUINCHO_24H"))
                .status(PolicyStatus.PENDING)
                .createdAt(Instant.now())
                .paymentResponseReceived(true)  // Já recebeu resposta
                .paymentConfirmed(true)
                .build();

        PaymentConfirmationEvent event = PaymentConfirmationEvent.builder()
                .policyRequestId(validPolicyId)
                .paymentStatus("APPROVED")
                .transactionId("TXN-12345")
                .build();

        when(objectMapper.readValue(validMessageBody, PaymentConfirmationEvent.class))
                .thenReturn(event);
        when(orderRepository.findById(any(PolicyProposalId.class)))
                .thenReturn(Optional.of(policyProposal));

        // When & Then
        assertThatThrownBy(() -> consumer.consumePaymentConfirmation(validMessageBody))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha ao processar mensagem de confirmação de pagamento")
                .hasCauseInstanceOf(IllegalStateException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve processar pagamento quando event não tem reason (aprovado)")
    void deveProcessarPagamentoQuandoEventNaoTemReasonAprovado() throws Exception {
        // Given
        PaymentConfirmationEvent event = PaymentConfirmationEvent.builder()
                .policyRequestId(validPolicyId)
                .paymentStatus("APPROVED")
                .transactionId("TXN-12345")
                .rejectionReason(null)  // Sem motivo pois foi aprovado
                .build();

        when(objectMapper.readValue(validMessageBody, PaymentConfirmationEvent.class))
                .thenReturn(event);
        when(orderRepository.findById(any(PolicyProposalId.class)))
                .thenReturn(Optional.of(policyProposal));
        when(orderRepository.save(any(PolicyProposal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        consumer.consumePaymentConfirmation(validMessageBody);

        // Then
        verify(orderRepository).save(any(PolicyProposal.class));
        verify(objectMapper).readValue(eq(validMessageBody), eq(PaymentConfirmationEvent.class));
    }
}
