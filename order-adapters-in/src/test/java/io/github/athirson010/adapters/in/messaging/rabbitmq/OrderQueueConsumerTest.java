package io.github.athirson010.adapters.in.messaging.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.athirson010.core.port.out.FraudCheckPort;
import io.github.athirson010.core.port.out.OrderEventPort;
import io.github.athirson010.core.port.out.OrderRepository;
import io.github.athirson010.core.service.PolicyValidationService;
import io.github.athirson010.domain.enums.*;
import io.github.athirson010.domain.model.FraudAnalysisResult;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderQueueConsumer - Testes Unitários")
class OrderQueueConsumerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private FraudCheckPort fraudCheckPort;

    @Mock
    private PolicyValidationService policyValidationService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventPort orderEventPort;

    @InjectMocks
    private OrderQueueConsumer orderQueueConsumer;

    private PolicyProposal policyProposal;
    private String messageBody;

    @BeforeEach
    void setUp() throws Exception {
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

        messageBody = "{\"id\":\"123\"}";
    }

    @Test
    @DisplayName("Deve processar inclusão com análise de fraude quando status é RECEIVED")
    void shouldProcessInclusionWhenStatusIsReceived() throws Exception {
        // Given
        FraudAnalysisResult fraudResult = FraudAnalysisResult.builder()
                .orderId(policyProposal.getId().value())
                .classification(RiskClassification.REGULAR)
                .occurrences(Collections.emptyList())
                .build();

        when(objectMapper.readValue(messageBody, PolicyProposal.class)).thenReturn(policyProposal);
        when(fraudCheckPort.analyzeFraud(any(PolicyProposal.class))).thenReturn(fraudResult);
        when(policyValidationService.validatePolicy(any(PolicyProposal.class), any(RiskClassification.class)))
                .thenReturn(true);

        // When
        orderQueueConsumer.consumeMessage(messageBody);

        // Then
        verify(objectMapper, times(1)).readValue(messageBody, PolicyProposal.class);
        verify(fraudCheckPort, times(1)).analyzeFraud(policyProposal);
        verify(policyValidationService, times(1))
                .validatePolicy(policyProposal, RiskClassification.REGULAR);
        verify(orderEventPort, times(1)).sendOrderApprovedEvent(any(PolicyProposal.class));
        verify(orderRepository, times(1)).save(any(PolicyProposal.class));
    }

    @Test
    @DisplayName("Deve processar cancelamento diretamente para Kafka quando status é CANCELED")
    void shouldProcessCancellationWhenStatusIsCanceled() throws Exception {
        // Given
        policyProposal.cancel("Cliente solicitou", java.time.Instant.now());
        when(objectMapper.readValue(messageBody, PolicyProposal.class)).thenReturn(policyProposal);

        // When
        orderQueueConsumer.consumeMessage(messageBody);

        // Then
        verify(objectMapper, times(1)).readValue(messageBody, PolicyProposal.class);
        verify(fraudCheckPort, never()).analyzeFraud(any(PolicyProposal.class));
        verify(policyValidationService, never())
                .validatePolicy(any(PolicyProposal.class), any(RiskClassification.class));
        verify(orderEventPort, times(1)).sendOrderCancelledEvent(policyProposal);
        verify(orderRepository, never()).save(any(PolicyProposal.class));
    }

    @Test
    @DisplayName("Deve rejeitar apólice quando validação falhar")
    void shouldRejectPolicyWhenValidationFails() throws Exception {
        // Given
        FraudAnalysisResult fraudResult = FraudAnalysisResult.builder()
                .orderId(policyProposal.getId().value())
                .classification(RiskClassification.HIGH_RISK)
                .occurrences(Collections.emptyList())
                .build();

        when(objectMapper.readValue(messageBody, PolicyProposal.class)).thenReturn(policyProposal);
        when(fraudCheckPort.analyzeFraud(any(PolicyProposal.class))).thenReturn(fraudResult);
        when(policyValidationService.validatePolicy(any(PolicyProposal.class), any(RiskClassification.class)))
                .thenReturn(false);

        // When
        orderQueueConsumer.consumeMessage(messageBody);

        // Then
        verify(objectMapper, times(1)).readValue(messageBody, PolicyProposal.class);
        verify(fraudCheckPort, times(1)).analyzeFraud(policyProposal);
        verify(policyValidationService, times(1))
                .validatePolicy(policyProposal, RiskClassification.HIGH_RISK);
        verify(orderEventPort, never()).sendOrderApprovedEvent(any(PolicyProposal.class));
        verify(orderRepository, times(1)).save(any(PolicyProposal.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando desserialização falhar")
    void shouldThrowExceptionWhenDeserializationFails() throws Exception {
        // Given
        when(objectMapper.readValue(messageBody, PolicyProposal.class))
                .thenThrow(new RuntimeException("Erro de desserialização"));

        // When/Then
        assertThatThrownBy(() -> orderQueueConsumer.consumeMessage(messageBody))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha ao processar mensagem");

        verify(objectMapper, times(1)).readValue(messageBody, PolicyProposal.class);
        verify(fraudCheckPort, never()).analyzeFraud(any(PolicyProposal.class));
        verify(orderEventPort, never()).sendOrderApprovedEvent(any(PolicyProposal.class));
        verify(orderEventPort, never()).sendOrderCancelledEvent(any(PolicyProposal.class));
    }

    @Test
    @DisplayName("Deve ignorar mensagens com status não reconhecido")
    void shouldIgnoreMessagesWithUnrecognizedStatus() throws Exception {
        // Given
        policyProposal.validate(java.time.Instant.now());
        policyProposal.reject("Motivo de rejeição", java.time.Instant.now());
        when(objectMapper.readValue(messageBody, PolicyProposal.class)).thenReturn(policyProposal);

        // When
        orderQueueConsumer.consumeMessage(messageBody);

        // Then
        verify(objectMapper, times(1)).readValue(messageBody, PolicyProposal.class);
        verify(fraudCheckPort, never()).analyzeFraud(any(PolicyProposal.class));
        verify(policyValidationService, never())
                .validatePolicy(any(PolicyProposal.class), any(RiskClassification.class));
        verify(orderEventPort, never()).sendOrderApprovedEvent(any(PolicyProposal.class));
        verify(orderEventPort, never()).sendOrderCancelledEvent(any(PolicyProposal.class));
    }

    @Test
    @DisplayName("Deve aprovar e enviar evento quando análise for bem-sucedida")
    void shouldApproveAndSendEventWhenAnalysisSucceeds() throws Exception {
        // Given
        FraudAnalysisResult fraudResult = FraudAnalysisResult.builder()
                .orderId(policyProposal.getId().value())
                .classification(RiskClassification.PREFERENTIAL)
                .occurrences(Collections.emptyList())
                .build();

        when(objectMapper.readValue(messageBody, PolicyProposal.class)).thenReturn(policyProposal);
        when(fraudCheckPort.analyzeFraud(any(PolicyProposal.class))).thenReturn(fraudResult);
        when(policyValidationService.validatePolicy(any(PolicyProposal.class), any(RiskClassification.class)))
                .thenReturn(true);

        // When
        orderQueueConsumer.consumeMessage(messageBody);

        // Then
        verify(fraudCheckPort, times(1)).analyzeFraud(policyProposal);
        verify(orderEventPort, times(1)).sendOrderApprovedEvent(any(PolicyProposal.class));
        verify(orderRepository, times(1)).save(any(PolicyProposal.class));
    }
}
