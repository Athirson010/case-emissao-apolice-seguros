package io.github.athirson010.adapters.out.messaging.rabbitmq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FraudQueueAdapter - Testes Unitários")
class FraudQueueAdapterTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private FraudQueueAdapter fraudQueueAdapter;

    private PolicyProposal policyProposal;
    private String exchange;
    private String routingKey;

    @BeforeEach
    void setUp() {
        exchange = "order-integration-exchange";
        routingKey = "order-routing-key";

        // Injeta os valores das propriedades usando ReflectionTestUtils
        ReflectionTestUtils.setField(fraudQueueAdapter, "exchange", exchange);
        ReflectionTestUtils.setField(fraudQueueAdapter, "routingKey", routingKey);

        policyProposal = PolicyProposal.builder()
                .id(PolicyProposalId.generate())
                .customerId(UUID.randomUUID())
                .productId("PROD-AUTO-2024")
                .category(Category.AUTO)
                .salesChannel(SalesChannel.MOBILE)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .totalMonthlyPremiumAmount(Money.brl(BigDecimal.valueOf(350.00)))
                .insuredAmount(Money.brl(BigDecimal.valueOf(200000.00)))
                .coverages(Map.of("COLISAO", Money.brl(BigDecimal.valueOf(200000.00))))
                .assistances(List.of("GUINCHO_24H"))
                .status(PolicyStatus.RECEIVED)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Deve enviar proposta de apólice para fila com sucesso")
    void deveEnviarPropostaDeApoliceParaFilaComSucesso() throws Exception {
        // Given
        String expectedMessage = "{\"id\":\"" + policyProposal.getId().asString() + "\"}";
        when(objectMapper.writeValueAsString(policyProposal))
                .thenReturn(expectedMessage);

        // When
        fraudQueueAdapter.sendToFraudQueue(policyProposal);

        // Then
        verify(objectMapper).writeValueAsString(policyProposal);
        verify(rabbitTemplate).convertAndSend(
                eq(exchange),
                eq(routingKey),
                eq(expectedMessage)
        );
    }

    @Test
    @DisplayName("Deve lançar exceção quando houver erro de serialização")
    void deveLancarExcecaoQuandoHouverErroDeSerializacao() throws Exception {
        // Given
        when(objectMapper.writeValueAsString(policyProposal))
                .thenThrow(new JsonProcessingException("Serialization error") {});

        // When & Then
        assertThatThrownBy(() -> fraudQueueAdapter.sendToFraudQueue(policyProposal))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha ao serializar PolicyProposal")
                .hasCauseInstanceOf(JsonProcessingException.class);

        verify(objectMapper).writeValueAsString(policyProposal);
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Deve lançar exceção quando houver erro ao publicar mensagem no RabbitMQ")
    void deveLancarExcecaoQuandoHouverErroAoPublicarMensagemNoRabbitMQ() throws Exception {
        // Given
        String expectedMessage = "{\"id\":\"" + policyProposal.getId().asString() + "\"}";
        when(objectMapper.writeValueAsString(policyProposal))
                .thenReturn(expectedMessage);

        doThrow(new RuntimeException("RabbitMQ connection error"))
                .when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), anyString());

        // When & Then
        assertThatThrownBy(() -> fraudQueueAdapter.sendToFraudQueue(policyProposal))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha ao enviar mensagem para fila order-service-consumer");

        verify(objectMapper).writeValueAsString(policyProposal);
        verify(rabbitTemplate).convertAndSend(eq(exchange), eq(routingKey), eq(expectedMessage));
    }

    @Test
    @DisplayName("Deve enviar proposta com status VALIDATED")
    void deveEnviarPropostaComStatusValidated() throws Exception {
        // Given
        policyProposal = PolicyProposal.builder()
                .id(PolicyProposalId.generate())
                .customerId(UUID.randomUUID())
                .productId("PROD-VIDA-2024")
                .category(Category.VIDA)
                .salesChannel(SalesChannel.WEB)
                .paymentMethod(PaymentMethod.PIX)
                .totalMonthlyPremiumAmount(Money.brl(BigDecimal.valueOf(500.00)))
                .insuredAmount(Money.brl(BigDecimal.valueOf(300000.00)))
                .coverages(Map.of("MORTE", Money.brl(BigDecimal.valueOf(300000.00))))
                .assistances(List.of("ASSISTENCIA_FUNERAL"))
                .status(PolicyStatus.VALIDATED)
                .createdAt(Instant.now())
                .build();

        String expectedMessage = "{\"id\":\"" + policyProposal.getId().asString() + "\"}";
        when(objectMapper.writeValueAsString(policyProposal))
                .thenReturn(expectedMessage);

        // When
        fraudQueueAdapter.sendToFraudQueue(policyProposal);

        // Then
        verify(objectMapper).writeValueAsString(policyProposal);
        verify(rabbitTemplate).convertAndSend(
                eq(exchange),
                eq(routingKey),
                eq(expectedMessage)
        );
    }

    @Test
    @DisplayName("Deve enviar proposta com múltiplas coberturas")
    void deveEnviarPropostaComMultiplasCoberturas() throws Exception {
        // Given
        policyProposal = PolicyProposal.builder()
                .id(PolicyProposalId.generate())
                .customerId(UUID.randomUUID())
                .productId("PROD-AUTO-PREMIUM-2024")
                .category(Category.AUTO)
                .salesChannel(SalesChannel.MOBILE)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .totalMonthlyPremiumAmount(Money.brl(BigDecimal.valueOf(800.00)))
                .insuredAmount(Money.brl(BigDecimal.valueOf(250000.00)))
                .coverages(Map.of(
                        "COLISAO", Money.brl(BigDecimal.valueOf(250000.00)),
                        "ROUBO", Money.brl(BigDecimal.valueOf(200000.00)),
                        "INCENDIO", Money.brl(BigDecimal.valueOf(150000.00))
                ))
                .assistances(List.of("GUINCHO_24H", "CARRO_RESERVA", "CHAVEIRO"))
                .status(PolicyStatus.RECEIVED)
                .createdAt(Instant.now())
                .build();

        String expectedMessage = "{\"id\":\"" + policyProposal.getId().asString() + "\"}";
        when(objectMapper.writeValueAsString(policyProposal))
                .thenReturn(expectedMessage);

        // When
        fraudQueueAdapter.sendToFraudQueue(policyProposal);

        // Then
        verify(objectMapper).writeValueAsString(policyProposal);
        verify(rabbitTemplate).convertAndSend(
                eq(exchange),
                eq(routingKey),
                eq(expectedMessage)
        );
    }

    @Test
    @DisplayName("Deve usar exchange e routing key corretos configurados")
    void deveUsarExchangeERoutingKeyCorretosConfigurados() throws Exception {
        // Given
        String customExchange = "custom-exchange";
        String customRoutingKey = "custom-routing-key";

        ReflectionTestUtils.setField(fraudQueueAdapter, "exchange", customExchange);
        ReflectionTestUtils.setField(fraudQueueAdapter, "routingKey", customRoutingKey);

        String expectedMessage = "{\"id\":\"" + policyProposal.getId().asString() + "\"}";
        when(objectMapper.writeValueAsString(policyProposal))
                .thenReturn(expectedMessage);

        // When
        fraudQueueAdapter.sendToFraudQueue(policyProposal);

        // Then
        verify(rabbitTemplate).convertAndSend(
                eq(customExchange),
                eq(customRoutingKey),
                eq(expectedMessage)
        );
    }

    @Test
    @DisplayName("Deve enviar proposta com todas as categorias de seguro")
    void deveEnviarPropostaComTodasAsCategoriasDeSeguro() throws Exception {
        // Given - Testa categoria RESIDENCIAL
        policyProposal = PolicyProposal.builder()
                .id(PolicyProposalId.generate())
                .customerId(UUID.randomUUID())
                .productId("PROD-RESIDENCIAL-2024")
                .category(Category.RESIDENCIAL)
                .salesChannel(SalesChannel.WEB)
                .paymentMethod(PaymentMethod.BOLETO)
                .totalMonthlyPremiumAmount(Money.brl(BigDecimal.valueOf(150.00)))
                .insuredAmount(Money.brl(BigDecimal.valueOf(100000.00)))
                .coverages(Map.of("INCENDIO", Money.brl(BigDecimal.valueOf(100000.00))))
                .assistances(List.of("ELETRICISTA", "ENCANADOR"))
                .status(PolicyStatus.RECEIVED)
                .createdAt(Instant.now())
                .build();

        String expectedMessage = "{\"id\":\"" + policyProposal.getId().asString() + "\"}";
        when(objectMapper.writeValueAsString(policyProposal))
                .thenReturn(expectedMessage);

        // When
        fraudQueueAdapter.sendToFraudQueue(policyProposal);

        // Then
        verify(objectMapper).writeValueAsString(policyProposal);
        verify(rabbitTemplate).convertAndSend(
                eq(exchange),
                eq(routingKey),
                eq(expectedMessage)
        );
    }
}
