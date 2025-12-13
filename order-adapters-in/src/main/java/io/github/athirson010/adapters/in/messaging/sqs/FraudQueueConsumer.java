package io.github.athirson010.adapters.in.messaging.sqs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.github.athirson010.core.port.out.FraudCheckPort;
import io.github.athirson010.core.port.out.OrderEventPort;
import io.github.athirson010.core.port.out.OrderRepository;
import io.github.athirson010.core.service.PolicyValidationService;
import io.github.athirson010.domain.model.FraudAnalysisResult;
import io.github.athirson010.domain.model.PolicyProposal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Profile("fraud-consumer")
@Component
@RequiredArgsConstructor
public class FraudQueueConsumer {

    private final ObjectMapper objectMapper;
    private final FraudCheckPort fraudCheckPort;
    private final PolicyValidationService policyValidationService;
    private final OrderRepository orderRepository;
    private final OrderEventPort orderEventPort;

    @SqsListener(value = "${aws.sqs.fraud-queue-name}")
    public void consumeMessage(Message<String> message) {
        try {
            log.info("Mensagem recebida da fila de fraude");

            String messageBody = message.getPayload();
            PolicyProposal policyProposal = deserializeMessage(messageBody);

            log.info("Proposta de apólice desserializada. ID: {}, Cliente: {}",
                    policyProposal.getId().asString(),
                    policyProposal.getCustomerId());

            FraudAnalysisResult analysisResult = fraudCheckPort.analyzeFraud(policyProposal);

            log.info("Análise de fraude concluída para pedido: {}. Classificação: {}, Ocorrências: {}",
                    analysisResult.getOrderId(),
                    analysisResult.getClassification(),
                    analysisResult.getOccurrences().size());

            processValidation(policyProposal, analysisResult);

        } catch (Exception e) {
            log.error("Erro ao processar mensagem da fila de fraude", e);
            throw new RuntimeException("Falha ao processar mensagem da fila de fraude", e);
        }
    }

    private void processValidation(PolicyProposal policyProposal, FraudAnalysisResult analysisResult) {
        Instant now = Instant.now();

        policyProposal.validate(now);
        log.info("Proposta de apólice {} marcada como VALIDADA", policyProposal.getId().asString());

        boolean isValid = policyValidationService.validatePolicy(
                policyProposal,
                analysisResult.getClassification()
        );

        if (isValid) {
            policyProposal.approve(now);
            log.info("Proposta de apólice {} APROVADA. Classificação: {}",
                    policyProposal.getId().asString(),
                    analysisResult.getClassification());

            orderEventPort.sendOrderApprovedEvent(policyProposal);
        } else {
            String reason = String.format(
                    "Apólice rejeitada devido ao capital segurado exceder o limite para cliente %s com categoria %s",
                    analysisResult.getClassification(),
                    policyProposal.getCategory()
            );
            policyProposal.reject(reason, now);
            log.info("Proposta de apólice {} REJEITADA. Motivo: {}",
                    policyProposal.getId().asString(),
                    reason);
        }

        orderRepository.save(policyProposal);
        log.info("Proposta de apólice {} salva com status: {}",
                policyProposal.getId().asString(),
                policyProposal.getStatus());
    }

    private PolicyProposal deserializeMessage(String messageBody) throws JsonProcessingException {
        return objectMapper.readValue(messageBody, PolicyProposal.class);
    }
}
