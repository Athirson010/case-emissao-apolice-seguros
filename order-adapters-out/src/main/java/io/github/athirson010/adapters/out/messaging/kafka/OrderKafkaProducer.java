package io.github.athirson010.adapters.out.messaging.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.athirson010.core.port.out.OrderEventPort;
import io.github.athirson010.domain.model.PolicyProposal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Profile("fraud-consumer")
@Component
@RequiredArgsConstructor
public class OrderKafkaProducer implements OrderEventPort {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.order}")
    private String orderTopic;

    @Override
    public void sendOrderApprovedEvent(PolicyProposal policyProposal) {
        try {
            log.info("Enviando evento de apólice aprovada para Kafka. ID: {}",
                    policyProposal.getId().asString());

            String message = objectMapper.writeValueAsString(policyProposal);
            String key = policyProposal.getId().asString();

            kafkaTemplate.send(orderTopic, key, message)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Evento de apólice aprovada enviado com sucesso para tópico: {}. ID: {}, Partition: {}, Offset: {}",
                                    orderTopic,
                                    policyProposal.getId().asString(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Erro ao enviar evento de apólice aprovada para Kafka. ID: {}",
                                    policyProposal.getId().asString(), ex);
                        }
                    });

        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar proposta de apólice para JSON. ID: {}",
                    policyProposal.getId().asString(), e);
            throw new RuntimeException("Falha ao serializar proposta de apólice para JSON", e);
        }
    }
}
