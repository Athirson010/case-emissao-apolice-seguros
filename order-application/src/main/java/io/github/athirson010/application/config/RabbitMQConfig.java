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
    public Queue orderConsumerQueue(
            @Value("${rabbitmq.queues.order-consumer}") String queue) {
        return QueueBuilder.durable(queue).build();
    }

    @Bean
    public Binding orderConsumerBinding(
            Queue orderConsumerQueue,
            TopicExchange orderIntegrationExchange,
            @Value("${rabbitmq.routing-keys.order}") String routingKey) {

        return BindingBuilder
                .bind(orderConsumerQueue)
                .to(orderIntegrationExchange)
                .with(routingKey);
    }
}
