package io.github.athirson010.adapters.in.messaging.rabbitmq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.athirson010.adapters.in.messaging.dto.SubscriptionConfirmationEvent;
import io.github.athirson010.core.port.out.OrderRepository;
import io.github.athirson010.domain.model.PolicyProposal;
import io.github.athirson010.domain.model.PolicyProposalId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Profile("order-response-insurance-consumer")
@Component
@RequiredArgsConstructor
public class InsuranceSubscriptionConfirmationConsumer {

    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;

    @RabbitListener(queues = "${rabbitmq.queues.subscription-confirmation}")
    public void consumeInsuranceSubscriptionConfirmation(String messageBody) {
        try {
            log.info("Mensagem de confirmação de subscrição de seguro recebida");

            SubscriptionConfirmationEvent event = deserializeMessage(messageBody);

            log.info("Evento de subscrição de seguro desserializado. PolicyId={}, Status={}",
                    event.getPolicyRequestId(),
                    event.getSubscriptionStatus());

            processInsuranceSubscriptionConfirmation(event);

        } catch (Exception e) {
            log.error("Erro ao processar mensagem de confirmação de subscrição de seguro", e);
            throw new RuntimeException("Falha ao processar mensagem de confirmação de subscrição de seguro", e);
        }
    }

    private void processInsuranceSubscriptionConfirmation(SubscriptionConfirmationEvent event) {
        PolicyProposalId policyId = PolicyProposalId.from(event.getPolicyRequestId());

        PolicyProposal policyProposal = orderRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Proposta de apólice não encontrada: " + event.getPolicyRequestId()));

        log.info("Proposta de apólice encontrada. PolicyId={}, Status atual={}",
                policyId.asString(),
                policyProposal.getStatus());

        Instant now = Instant.now();

        // Processa a resposta da subscrição de seguro (aprovada ou rejeitada)
        boolean approved = event.isApproved();
        String rejectionReason = event.getRejectionReason();

        log.info("Processando resposta de SUBSCRIÇÃO DE SEGURO. PolicyId={}, Status={}, SubscriptionId={}",
                policyId.asString(),
                approved ? "APPROVED" : "REJECTED",
                event.getSubscriptionId());

        // Nova lógica: só aprova/rejeita quando AMBAS respostas chegarem
        policyProposal.processSubscriptionResponse(approved, rejectionReason, now);

        orderRepository.save(policyProposal);

        log.info("Resposta de subscrição de seguro processada. PolicyId={}, Status final={}",
                policyId.asString(),
                policyProposal.getStatus());
    }

    private SubscriptionConfirmationEvent deserializeMessage(String messageBody)
            throws JsonProcessingException {
        return objectMapper.readValue(messageBody, SubscriptionConfirmationEvent.class);
    }
}
