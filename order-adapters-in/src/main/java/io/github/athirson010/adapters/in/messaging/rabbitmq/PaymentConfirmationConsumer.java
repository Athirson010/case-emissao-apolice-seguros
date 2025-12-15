package io.github.athirson010.adapters.in.messaging.rabbitmq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.athirson010.adapters.in.messaging.dto.PaymentConfirmationEvent;
import io.github.athirson010.core.port.out.OrderRepository;
import io.github.athirson010.domain.enums.PolicyStatus;
import io.github.athirson010.domain.model.PolicyProposal;
import io.github.athirson010.domain.model.PolicyProposalId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Profile("order-consumer")
@Component
@RequiredArgsConstructor
public class PaymentConfirmationConsumer {

    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;

    @RabbitListener(queues = "${rabbitmq.queues.payment-confirmation}")
    public void consumePaymentConfirmation(String messageBody) {
        try {
            log.info("Mensagem de confirmação de pagamento recebida");

            PaymentConfirmationEvent event = deserializeMessage(messageBody);

            log.info("Evento de pagamento desserializado. PolicyId={}, Status={}",
                    event.getPolicyRequestId(),
                    event.getPaymentStatus());

            processPaymentConfirmation(event);

        } catch (Exception e) {
            log.error("Erro ao processar mensagem de confirmação de pagamento", e);
            throw new RuntimeException("Falha ao processar mensagem de confirmação de pagamento", e);
        }
    }

    private void processPaymentConfirmation(PaymentConfirmationEvent event) {
        PolicyProposalId policyId = PolicyProposalId.from(event.getPolicyRequestId());

        PolicyProposal policyProposal = orderRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Proposta de apólice não encontrada: " + event.getPolicyRequestId()));

        log.info("Proposta de apólice encontrada. PolicyId={}, Status atual={}",
                policyId.asString(),
                policyProposal.getStatus());

        if (policyProposal.getStatus() != PolicyStatus.PENDING) {
            log.warn("Proposta não está em estado PENDING. PolicyId={}, Status={}",
                    policyId.asString(),
                    policyProposal.getStatus());
            return;
        }

        Instant now = Instant.now();

        // Processa a resposta do pagamento (aprovado ou rejeitado)
        boolean approved = event.isApproved();
        String rejectionReason = event.getRejectionReason();

        log.info("Processando resposta de PAGAMENTO. PolicyId={}, Status={}, TransactionId={}",
                policyId.asString(),
                approved ? "APPROVED" : "REJECTED",
                event.getTransactionId());

        // Nova lógica: só aprova/rejeita quando AMBAS respostas chegarem
        policyProposal.processPaymentResponse(approved, rejectionReason, now);

        orderRepository.save(policyProposal);

        log.info("Resposta de pagamento processada. PolicyId={}, Status final={}",
                policyId.asString(),
                policyProposal.getStatus());
    }

    private PaymentConfirmationEvent deserializeMessage(String messageBody)
            throws JsonProcessingException {
        return objectMapper.readValue(messageBody, PaymentConfirmationEvent.class);
    }
}
