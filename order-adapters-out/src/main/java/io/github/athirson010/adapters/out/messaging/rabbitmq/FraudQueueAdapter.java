package io.github.athirson010.adapters.out.messaging.rabbitmq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.athirson010.core.port.out.FraudQueuePort;
import io.github.athirson010.domain.model.PolicyProposal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Profile("api")
@Component
@RequiredArgsConstructor
public class FraudQueueAdapter implements FraudQueuePort {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rabbitmq.exchanges.order-integration}")
    private String exchange;

    @Value("${rabbitmq.routing-keys.fraud}")
    private String routingKey;

    @Override
    public void sendToFraudQueue(PolicyProposal policyProposal) {
        try {
            log.debug("Enviando proposta de apólice para fila de fraude. PolicyId={}",
                    policyProposal.getId().asString());

            String message = objectMapper.writeValueAsString(policyProposal);

            rabbitTemplate.convertAndSend(exchange, routingKey, message);

            log.info("Proposta enviada para fraude com sucesso. PolicyId={}",
                    policyProposal.getId().asString());

        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar PolicyProposal. PolicyId={}",
                    policyProposal.getId().asString(), e);
            throw new RuntimeException("Falha ao serializar PolicyProposal", e);
        } catch (Exception e) {
            log.error("Erro ao publicar mensagem no RabbitMQ. PolicyId={}",
                    policyProposal.getId().asString(), e);
            throw new RuntimeException("Falha ao enviar mensagem para fila de fraude", e);
        }
    }
}
