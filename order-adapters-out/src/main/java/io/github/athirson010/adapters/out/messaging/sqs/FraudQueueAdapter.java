package io.github.athirson010.adapters.out.messaging.sqs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.athirson010.core.port.out.FraudQueuePort;
import io.github.athirson010.domain.model.PolicyProposal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Slf4j
@Profile("api")
@Component
@RequiredArgsConstructor
public class FraudQueueAdapter implements FraudQueuePort {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.fraud-queue-name}")
    private String fraudQueueName;

    @Override
    public void sendToFraudQueue(PolicyProposal policyProposal) {
        try {
            log.debug("Enviando proposta de apólice para fila de fraude: {}", policyProposal.getId().asString());

            String queueUrl = getQueueUrl(fraudQueueName);
            String messageBody = objectMapper.writeValueAsString(policyProposal);

            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .build();

            SendMessageResponse response = sqsClient.sendMessage(sendMessageRequest);

            log.info("Proposta de apólice enviada para fila de fraude com sucesso. MessageId: {}, PolicyId: {}",
                    response.messageId(), policyProposal.getId().asString());

        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar proposta de apólice para JSON: {}", policyProposal.getId().asString(), e);
            throw new RuntimeException("Falha ao serializar proposta de apólice para JSON", e);
        } catch (Exception e) {
            log.error("Erro ao enviar mensagem para fila SQS de fraude: {}", policyProposal.getId().asString(), e);
            throw new RuntimeException("Falha ao enviar mensagem para fila de fraude", e);
        }
    }

    private String getQueueUrl(String queueName) {
        GetQueueUrlRequest getQueueUrlRequest = GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build();

        return sqsClient.getQueueUrl(getQueueUrlRequest).queueUrl();
    }
}
