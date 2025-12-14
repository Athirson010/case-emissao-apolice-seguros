package io.github.athirson010.adapters.in.messaging.rabbitmq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.athirson010.core.port.out.FraudCheckPort;
import io.github.athirson010.core.port.out.OrderEventPort;
import io.github.athirson010.core.port.out.OrderRepository;
import io.github.athirson010.core.service.PolicyValidationService;
import io.github.athirson010.domain.enums.PolicyStatus;
import io.github.athirson010.domain.model.FraudAnalysisResult;
import io.github.athirson010.domain.model.PolicyProposal;
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
public class OrderQueueConsumer {

    private final ObjectMapper objectMapper;
    private final FraudCheckPort fraudCheckPort;
    private final PolicyValidationService policyValidationService;
    private final OrderRepository orderRepository;
    private final OrderEventPort orderEventPort;

    @RabbitListener(queues = "${rabbitmq.queues.order-consumer}")
    public void consumeMessage(String messageBody) {
        try {
            log.info("Mensagem recebida da fila order-service-consumer");

            PolicyProposal policyProposal = deserializeMessage(messageBody);

            log.info("Proposta desserializada. PolicyId={}, CustomerId={}, Status={}",
                    policyProposal.getId().asString(),
                    policyProposal.getCustomerId(),
                    policyProposal.getStatus());

            if (PolicyStatus.RECEIVED.equals(policyProposal.getStatus())) {
                log.info("Processando inclusão de apólice - iniciando validação de fraude");
                processInclusion(policyProposal);
            } else if (PolicyStatus.CANCELED.equals(policyProposal.getStatus())) {
                log.info("Processando cancelamento de apólice - enviando direto para Kafka");
                processCancellation(policyProposal);
            } else {
                log.warn("Status não reconhecido para processamento: {}. PolicyId={}",
                        policyProposal.getStatus(),
                        policyProposal.getId().asString());
            }

        } catch (Exception e) {
            log.error("Erro ao processar mensagem da fila order-service-consumer", e);
            throw new RuntimeException("Falha ao processar mensagem da fila order-service-consumer", e);
            // RabbitMQ: exception = requeue ou DLQ (dependendo config)
        }
    }

    private void processInclusion(PolicyProposal policyProposal) {
        log.info("Iniciando análise de fraude para apólice: {}", policyProposal.getId().asString());

        FraudAnalysisResult analysisResult = fraudCheckPort.analyzeFraud(policyProposal);

        log.info("Análise concluída. PolicyId={}, Classificação={}, Ocorrências={}",
                analysisResult.getOrderId(),
                analysisResult.getClassification(),
                analysisResult.getOccurrences().size());

        processValidation(policyProposal, analysisResult);
    }

    private void processCancellation(PolicyProposal policyProposal) {
        log.info("Publicando evento de cancelamento no Kafka para apólice: {}", policyProposal.getId().asString());

        orderEventPort.sendOrderCancelledEvent(policyProposal);

        log.info("Evento de cancelamento publicado com sucesso. PolicyId={}", policyProposal.getId().asString());
    }

    private void processValidation(
            PolicyProposal policyProposal,
            FraudAnalysisResult analysisResult
    ) {
        Instant now = Instant.now();

        policyProposal.validate(now);
        log.info("Policy {} marcada como VALIDADA", policyProposal.getId().asString());

        boolean isValid = policyValidationService.validatePolicy(
                policyProposal,
                analysisResult.getClassification()
        );

        if (isValid) {
            policyProposal.approve(now);

            log.info("Policy {} APROVADA. Classificação={}",
                    policyProposal.getId().asString(),
                    analysisResult.getClassification());

            orderEventPort.sendOrderApprovedEvent(policyProposal);

        } else {
            String reason = String.format(
                    "Apólice rejeitada. Categoria=%s, Classificação=%s",
                    policyProposal.getCategory(),
                    analysisResult.getClassification()
            );

            policyProposal.reject(reason, now);

            log.info("Policy {} REJEITADA. Motivo={}",
                    policyProposal.getId().asString(),
                    reason);
        }

        orderRepository.save(policyProposal);

        log.info("Policy {} persistida com status={}",
                policyProposal.getId().asString(),
                policyProposal.getStatus());
    }

    private PolicyProposal deserializeMessage(String messageBody)
            throws JsonProcessingException {

        return objectMapper.readValue(messageBody, PolicyProposal.class);
    }
}
