package io.github.athirson010.adapters.out.messaging.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.athirson010.domain.enums.Category;
import io.github.athirson010.domain.enums.PaymentMethod;
import io.github.athirson010.domain.enums.SalesChannel;
import io.github.athirson010.domain.model.Money;
import io.github.athirson010.domain.model.PolicyProposal;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderKafkaProducer - Testes Unitários")
class OrderKafkaProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderKafkaProducer orderKafkaProducer;

    private PolicyProposal policyProposal;
    private String orderTopic = "order-topic";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderKafkaProducer, "orderTopic", orderTopic);

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
    }

    @Test
    @DisplayName("Deve enviar evento de apólice aprovada com sucesso")
    void shouldSendOrderApprovedEventSuccessfully() throws JsonProcessingException {
        // Given
        String messageJson = "{\"id\":\"123\"}";
        CompletableFuture<SendResult<String, String>> future = createSuccessfulFuture();

        when(objectMapper.writeValueAsString(policyProposal)).thenReturn(messageJson);
        when(kafkaTemplate.send(eq(orderTopic), anyString(), eq(messageJson)))
                .thenReturn(future);

        // When
        orderKafkaProducer.sendOrderApprovedEvent(policyProposal);

        // Then
        verify(objectMapper, times(1)).writeValueAsString(policyProposal);
        verify(kafkaTemplate, times(1)).send(eq(orderTopic), anyString(), eq(messageJson));
    }

    @Test
    @DisplayName("Deve enviar evento de cancelamento com sucesso")
    void shouldSendOrderCancelledEventSuccessfully() throws JsonProcessingException {
        // Given
        policyProposal.cancel("Cliente solicitou", java.time.Instant.now());
        String messageJson = "{\"id\":\"123\",\"status\":\"CANCELED\"}";
        CompletableFuture<SendResult<String, String>> future = createSuccessfulFuture();

        when(objectMapper.writeValueAsString(policyProposal)).thenReturn(messageJson);
        when(kafkaTemplate.send(eq(orderTopic), anyString(), eq(messageJson)))
                .thenReturn(future);

        // When
        orderKafkaProducer.sendOrderCancelledEvent(policyProposal);

        // Then
        verify(objectMapper, times(1)).writeValueAsString(policyProposal);
        verify(kafkaTemplate, times(1)).send(eq(orderTopic), anyString(), eq(messageJson));
    }

    @Test
    @DisplayName("Deve lançar exceção quando serialização falhar ao aprovar")
    void shouldThrowExceptionWhenSerializationFailsOnApprove() throws JsonProcessingException {
        // Given
        when(objectMapper.writeValueAsString(policyProposal))
                .thenThrow(new JsonProcessingException("Erro de serialização") {
                });

        // When/Then
        assertThatThrownBy(() -> orderKafkaProducer.sendOrderApprovedEvent(policyProposal))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha ao serializar");

        verify(objectMapper, times(1)).writeValueAsString(policyProposal);
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Deve lançar exceção quando serialização falhar ao cancelar")
    void shouldThrowExceptionWhenSerializationFailsOnCancel() throws JsonProcessingException {
        // Given
        policyProposal.cancel("Motivo", java.time.Instant.now());
        when(objectMapper.writeValueAsString(policyProposal))
                .thenThrow(new JsonProcessingException("Erro de serialização") {
                });

        // When/Then
        assertThatThrownBy(() -> orderKafkaProducer.sendOrderCancelledEvent(policyProposal))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha ao serializar");

        verify(objectMapper, times(1)).writeValueAsString(policyProposal);
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Deve tratar erro de envio ao Kafka graciosamente")
    void shouldHandleKafkaSendErrorGracefully() throws JsonProcessingException {
        // Given
        String messageJson = "{\"id\":\"123\"}";
        CompletableFuture<SendResult<String, String>> future = createFailedFuture();

        when(objectMapper.writeValueAsString(policyProposal)).thenReturn(messageJson);
        when(kafkaTemplate.send(eq(orderTopic), anyString(), eq(messageJson)))
                .thenReturn(future);

        // When
        orderKafkaProducer.sendOrderApprovedEvent(policyProposal);

        // Then
        verify(objectMapper, times(1)).writeValueAsString(policyProposal);
        verify(kafkaTemplate, times(1)).send(eq(orderTopic), anyString(), eq(messageJson));
    }

    // ========== MÉTODOS AUXILIARES ==========

    private CompletableFuture<SendResult<String, String>> createSuccessfulFuture() {
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();

        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(orderTopic, "key", "value");
        RecordMetadata recordMetadata = new RecordMetadata(
                new TopicPartition(orderTopic, 0),
                0L,
                0,
                System.currentTimeMillis(),
                0L,
                0,
                0
        );

        SendResult<String, String> sendResult = new SendResult<>(producerRecord, recordMetadata);
        future.complete(sendResult);

        return future;
    }

    private CompletableFuture<SendResult<String, String>> createFailedFuture() {
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka error"));
        return future;
    }
}
