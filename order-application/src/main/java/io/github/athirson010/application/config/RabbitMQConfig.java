package io.github.athirson010.application.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange orderIntegrationExchange(
            @Value("${rabbitmq.exchanges.order-integration}") String exchange) {
        return new TopicExchange(exchange);
    }

    @Bean
    public Queue fraudQueue(
            @Value("${rabbitmq.queues.fraud}") String queue) {
        return QueueBuilder.durable(queue).build();
    }

    @Bean
    public Binding fraudBinding(
            Queue fraudQueue,
            TopicExchange orderIntegrationExchange,
            @Value("${rabbitmq.routing-keys.fraud}") String routingKey) {

        return BindingBuilder
                .bind(fraudQueue)
                .to(orderIntegrationExchange)
                .with(routingKey);
    }
}
