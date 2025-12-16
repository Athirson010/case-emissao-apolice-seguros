package io.github.athirson010.adapters.in.messaging.rabbitmq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.athirson010.adapters.in.messaging.dto.SubscriptionConfirmationEvent;
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
@DisplayName("InsuranceSubscriptionConfirmationConsumer - Testes Unitários")
class InsuranceSubscriptionConfirmationConsumerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private InsuranceSubscriptionConfirmationConsumer consumer;

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
                .paymentResponseReceived(true)
                .paymentConfirmed(true)
                .build();

        validMessageBody = "{\"policy_request_id\":\"" + validPolicyId + "\",\"subscription_status\":\"APPROVED\"}";
    }

    @Test
    @DisplayName("Deve processar confirmação de subscrição aprovada com sucesso")
    void deveProcessarConfirmacaoDeSubscricaoAprovadaComSucesso() throws Exception {
        // Given
        SubscriptionConfirmationEvent event = SubscriptionConfirmationEvent.builder()
                .policyRequestId(validPolicyId)
                .subscriptionStatus("APPROVED")
                .subscriptionId("SUB-12345")
                .authorizationTimestamp(Instant.now())
                .build();

        when(objectMapper.readValue(validMessageBody, SubscriptionConfirmationEvent.class))
                .thenReturn(event);
        when(orderRepository.findById(any(PolicyProposalId.class)))
                .thenReturn(Optional.of(policyProposal));
        when(orderRepository.save(any(PolicyProposal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        consumer.consumeInsuranceSubscriptionConfirmation(validMessageBody);

        // Then
        ArgumentCaptor<PolicyProposal> captor = ArgumentCaptor.forClass(PolicyProposal.class);
        verify(orderRepository).save(captor.capture());

        PolicyProposal savedProposal = captor.getValue();
        assertThat(savedProposal.getStatus()).isEqualTo(PolicyStatus.APPROVED);
        assertThat(savedProposal.isSubscriptionResponseReceived()).isTrue();
        assertThat(savedProposal.isSubscriptionConfirmed()).isTrue();
        assertThat(savedProposal.getFinishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve processar confirmação de subscrição rejeitada com sucesso")
    void deveProcessarConfirmacaoDeSubscricaoRejeitadaComSucesso() throws Exception {
        // Given
        String rejectionReason = "Perfil de risco não aceito";
        SubscriptionConfirmationEvent event = SubscriptionConfirmationEvent.builder()
                .policyRequestId(validPolicyId)
                .subscriptionStatus("REJECTED")
                .subscriptionId("SUB-12345")
                .authorizationTimestamp(Instant.now())
                .rejectionReason(rejectionReason)
                .build();

        String messageBody = "{\"policy_request_id\":\"" + validPolicyId + "\",\"subscription_status\":\"REJECTED\"}";

        when(objectMapper.readValue(messageBody, SubscriptionConfirmationEvent.class))
                .thenReturn(event);
        when(orderRepository.findById(any(PolicyProposalId.class)))
                .thenReturn(Optional.of(policyProposal));
        when(orderRepository.save(any(PolicyProposal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        consumer.consumeInsuranceSubscriptionConfirmation(messageBody);

        // Then
        ArgumentCaptor<PolicyProposal> captor = ArgumentCaptor.forClass(PolicyProposal.class);
        verify(orderRepository).save(captor.capture());

        PolicyProposal savedProposal = captor.getValue();
        assertThat(savedProposal.getStatus()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(savedProposal.isSubscriptionResponseReceived()).isTrue();
        assertThat(savedProposal.isSubscriptionConfirmed()).isFalse();
        assertThat(savedProposal.getSubscriptionRejectionReason()).isEqualTo(rejectionReason);
        assertThat(savedProposal.getFinishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve lançar exceção quando proposta de apólice não for encontrada")
    void deveLancarExcecaoQuandoPropostaDeApoliceNaoForEncontrada() throws Exception {
        // Given
        SubscriptionConfirmationEvent event = SubscriptionConfirmationEvent.builder()
                .policyRequestId(validPolicyId)
                .subscriptionStatus("APPROVED")
                .subscriptionId("SUB-12345")
                .build();

        when(objectMapper.readValue(validMessageBody, SubscriptionConfirmationEvent.class))
                .thenReturn(event);
        when(orderRepository.findById(any(PolicyProposalId.class)))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> consumer.consumeInsuranceSubscriptionConfirmation(validMessageBody))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha ao processar mensagem de confirmação de subscrição de seguro")
                .hasCauseInstanceOf(IllegalArgumentException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando houver erro de desserialização")
    void deveLancarExcecaoQuandoHouverErroDeDesserializacao() throws Exception {
        // Given
        String invalidMessageBody = "invalid json";

        when(objectMapper.readValue(invalidMessageBody, SubscriptionConfirmationEvent.class))
                .thenThrow(new JsonProcessingException("Invalid JSON") {});

        // When & Then
        assertThatThrownBy(() -> consumer.consumeInsuranceSubscriptionConfirmation(invalidMessageBody))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha ao processar mensagem de confirmação de subscrição de seguro")
                .hasCauseInstanceOf(JsonProcessingException.class);

        verify(orderRepository, never()).findById(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve processar subscrição aprovada mesmo quando pagamento ainda não foi respondido")
    void deveProcessarSubscricaoAprovadaMesmoQuandoPagamentoAindaNaoFoiRespondido() throws Exception {
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
                .paymentResponseReceived(false)  // Pagamento ainda não respondeu
                .paymentConfirmed(false)
                .build();

        SubscriptionConfirmationEvent event = SubscriptionConfirmationEvent.builder()
                .policyRequestId(validPolicyId)
                .subscriptionStatus("APPROVED")
                .subscriptionId("SUB-12345")
                .build();

        when(objectMapper.readValue(validMessageBody, SubscriptionConfirmationEvent.class))
                .thenReturn(event);
        when(orderRepository.findById(any(PolicyProposalId.class)))
                .thenReturn(Optional.of(policyProposal));
        when(orderRepository.save(any(PolicyProposal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        consumer.consumeInsuranceSubscriptionConfirmation(validMessageBody);

        // Then
        ArgumentCaptor<PolicyProposal> captor = ArgumentCaptor.forClass(PolicyProposal.class);
        verify(orderRepository).save(captor.capture());

        PolicyProposal savedProposal = captor.getValue();
        // Deve permanecer PENDING aguardando resposta de pagamento
        assertThat(savedProposal.getStatus()).isEqualTo(PolicyStatus.PENDING);
        assertThat(savedProposal.isSubscriptionResponseReceived()).isTrue();
        assertThat(savedProposal.isSubscriptionConfirmed()).isTrue();
        assertThat(savedProposal.getFinishedAt()).isNull();
    }

    @Test
    @DisplayName("Deve processar subscrição após policy já ter sido rejeitada por pagamento")
    void deveProcessarSubscricaoAposPoliceJaTerSidoRejeitadaPorPagamento() throws Exception {
        // Given - Policy já foi rejeitada por pagamento
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
                .paymentResponseReceived(true)
                .paymentConfirmed(false)
                .paymentRejectionReason("Cartão recusado")
                .build();

        SubscriptionConfirmationEvent event = SubscriptionConfirmationEvent.builder()
                .policyRequestId(validPolicyId)
                .subscriptionStatus("APPROVED")
                .subscriptionId("SUB-12345")
                .build();

        when(objectMapper.readValue(validMessageBody, SubscriptionConfirmationEvent.class))
                .thenReturn(event);
        when(orderRepository.findById(any(PolicyProposalId.class)))
                .thenReturn(Optional.of(policyProposal));
        when(orderRepository.save(any(PolicyProposal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        consumer.consumeInsuranceSubscriptionConfirmation(validMessageBody);

        // Then
        ArgumentCaptor<PolicyProposal> captor = ArgumentCaptor.forClass(PolicyProposal.class);
        verify(orderRepository).save(captor.capture());

        PolicyProposal savedProposal = captor.getValue();
        // Deve permanecer REJECTED
        assertThat(savedProposal.getStatus()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(savedProposal.isSubscriptionResponseReceived()).isTrue();
        assertThat(savedProposal.isSubscriptionConfirmed()).isTrue();
    }

    @Test
    @DisplayName("Deve lançar exceção quando resposta de subscrição já foi recebida")
    void deveLancarExcecaoQuandoRespostaDeSubscricaoJaFoiRecebida() throws Exception {
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
                .subscriptionResponseReceived(true)  // Já recebeu resposta
                .subscriptionConfirmed(true)
                .build();

        SubscriptionConfirmationEvent event = SubscriptionConfirmationEvent.builder()
                .policyRequestId(validPolicyId)
                .subscriptionStatus("APPROVED")
                .subscriptionId("SUB-12345")
                .build();

        when(objectMapper.readValue(validMessageBody, SubscriptionConfirmationEvent.class))
                .thenReturn(event);
        when(orderRepository.findById(any(PolicyProposalId.class)))
                .thenReturn(Optional.of(policyProposal));

        // When & Then
        assertThatThrownBy(() -> consumer.consumeInsuranceSubscriptionConfirmation(validMessageBody))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha ao processar mensagem de confirmação de subscrição de seguro")
                .hasCauseInstanceOf(IllegalStateException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve processar subscrição quando event não tem reason (aprovado)")
    void deveProcessarSubscricaoQuandoEventNaoTemReasonAprovado() throws Exception {
        // Given
        SubscriptionConfirmationEvent event = SubscriptionConfirmationEvent.builder()
                .policyRequestId(validPolicyId)
                .subscriptionStatus("APPROVED")
                .subscriptionId("SUB-12345")
                .rejectionReason(null)  // Sem motivo pois foi aprovado
                .build();

        when(objectMapper.readValue(validMessageBody, SubscriptionConfirmationEvent.class))
                .thenReturn(event);
        when(orderRepository.findById(any(PolicyProposalId.class)))
                .thenReturn(Optional.of(policyProposal));
        when(orderRepository.save(any(PolicyProposal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        consumer.consumeInsuranceSubscriptionConfirmation(validMessageBody);

        // Then
        verify(orderRepository).save(any(PolicyProposal.class));
        verify(objectMapper).readValue(eq(validMessageBody), eq(SubscriptionConfirmationEvent.class));
    }
}
