package io.github.athirson010.adapters.in.messaging.rabbitmq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.athirson010.adapters.in.messaging.dto.SubscriptionConfirmationEvent;
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
public class SubscriptionConfirmationConsumer {

    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;

    @RabbitListener(queues = "${rabbitmq.queues.subscription-confirmation}")
    public void consumeSubscriptionConfirmation(String messageBody) {
        try {
            log.info("Mensagem de confirmação de subscrição recebida");

            SubscriptionConfirmationEvent event = deserializeMessage(messageBody);

            log.info("Evento de subscrição desserializado. PolicyId={}, Status={}",
                    event.getPolicyRequestId(),
                    event.getSubscriptionStatus());

            processSubscriptionConfirmation(event);

        } catch (Exception e) {
            log.error("Erro ao processar mensagem de confirmação de subscrição", e);
            throw new RuntimeException("Falha ao processar mensagem de confirmação de subscrição", e);
        }
    }

    private void processSubscriptionConfirmation(SubscriptionConfirmationEvent event) {
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

        if (event.isApproved()) {
            log.info("Subscrição APROVADA. PolicyId={}, SubscriptionId={}",
                    policyId.asString(),
                    event.getSubscriptionId());

            policyProposal.confirmSubscription(now);

            orderRepository.save(policyProposal);

            log.info("Confirmação de subscrição processada. PolicyId={}, Status={}",
                    policyId.asString(),
                    policyProposal.getStatus());

        } else if (event.isRejected()) {
            log.info("Subscrição REJEITADA. PolicyId={}, Motivo={}",
                    policyId.asString(),
                    event.getRejectionReason());

            String reason = String.format("Subscrição rejeitada: %s", event.getRejectionReason());
            policyProposal.reject(reason, now);

            orderRepository.save(policyProposal);

            log.info("Proposta rejeitada por falha na subscrição. PolicyId={}", policyId.asString());
        }
    }

    private SubscriptionConfirmationEvent deserializeMessage(String messageBody)
            throws JsonProcessingException {
        return objectMapper.readValue(messageBody, SubscriptionConfirmationEvent.class);
    }
}
