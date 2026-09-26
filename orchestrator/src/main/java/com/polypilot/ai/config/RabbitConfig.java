package com.polypilot.ai.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the {@code ai.signals} queue this orchestrator consumes (previously
 * never declared anywhere — {@code InfrastructureService.rabbitmq()}'s health
 * probe reported "not declared" until this lands), plus a bounded-retry +
 * dead-letter setup so a malformed/unparseable message can't crash the
 * listener or loop forever.
 *
 * <p>A message that fails {@link org.springframework.amqp.rabbit.annotation.RabbitListener}
 * processing is retried up to {@link #MAX_RETRY_ATTEMPTS} times in-process, then
 * rejected without requeue — which RabbitMQ routes to {@code ai.signals.dlq} via
 * the primary queue's dead-letter-exchange argument, rather than redelivering
 * forever.
 */
@Configuration
public class RabbitConfig {

    private static final int MAX_RETRY_ATTEMPTS = 3;

    private static final String DLX_NAME = "ai.signals.dlx";
    private static final String DLQ_ROUTING_KEY = "ai.signals.dlq";

    @Bean
    public Queue aiSignalsQueue(@Value("${polypilot.ai.signals-queue:ai.signals}") String queueName) {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange aiSignalsDeadLetterExchange() {
        return new DirectExchange(DLX_NAME);
    }

    @Bean
    public Queue aiSignalsDeadLetterQueue(@Value("${polypilot.ai.signals-queue:ai.signals}") String queueName) {
        return QueueBuilder.durable(queueName + ".dlq").build();
    }

    @Bean
    public Binding aiSignalsDeadLetterBinding(Queue aiSignalsDeadLetterQueue, DirectExchange aiSignalsDeadLetterExchange) {
        return BindingBuilder.bind(aiSignalsDeadLetterQueue).to(aiSignalsDeadLetterExchange).with(DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setAdviceChain(retryInterceptor());
        return factory;
    }

    private MethodInterceptor retryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxRetries(MAX_RETRY_ATTEMPTS)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }
}
