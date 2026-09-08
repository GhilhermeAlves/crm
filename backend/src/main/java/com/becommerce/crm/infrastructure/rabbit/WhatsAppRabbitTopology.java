package com.becommerce.crm.infrastructure.rabbit;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia do fluxo WhatsApp/UAZAPI (Sprint 23).
 *
 * <p>Uma única exchange tópica {@code crm.whatsapp} roteia os eventos por tipo
 * (inbound / auto-ai / sender / followup.executor). Toda fila crítica possui DLQ
 * ({@code *.dlq}) via dead-letter exchange {@code crm.whatsapp.dlx}: após as
 * tentativas de retry esgotadas ({@code spring.rabbitmq.listener.simple.retry.*})
 * e rejeição, a mensagem é roteada automaticamente para a DLQ — nunca excluída.
 *
 * <p>A topologia é DECLARADA pelo RabbitAdmin do Spring Boot a partir destes
 * beans (production → broker da VPS; o definitions.json vazio não interfere).
 */
@Configuration
public class WhatsAppRabbitTopology {

    @Bean
    public TopicExchange whatsappExchange() {
        return new TopicExchange(RabbitTopics.WHATSAPP_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange whatsappDeadLetterExchange() {
        return new DirectExchange(RabbitTopics.DEAD_LETTER_EXCHANGE, true, false);
    }

    // -----------------------------------------------------------------------
    // Filas principais (durable, com dead-letter para a DLX única)
    // -----------------------------------------------------------------------

    @Bean
    public Queue whatsappInboundQueue() {
        return primaryQueue(RabbitTopics.INBOUND_QUEUE);
    }

    @Bean
    public Queue whatsappAutoAiQueue() {
        return primaryQueue(RabbitTopics.AUTO_AI_QUEUE);
    }

    @Bean
    public Queue whatsappSenderQueue() {
        return primaryQueue(RabbitTopics.SENDER_QUEUE);
    }

    @Bean
    public Queue followupExecutorQueue() {
        return primaryQueue(RabbitTopics.FOLLOWUP_EXECUTOR_QUEUE);
    }

    /** Dead-letter queue de cada fila principal (uniquamente consumida por diagnósticos). */
    @Bean
    public Queue whatsappInboundDlq() {
        return dlq(RabbitTopics.INBOUND_DLQ);
    }

    @Bean
    public Queue whatsappAutoAiDlq() {
        return dlq(RabbitTopics.AUTO_AI_DLQ);
    }

    @Bean
    public Queue whatsappSenderDlq() {
        return dlq(RabbitTopics.SENDER_DLQ);
    }

    @Bean
    public Queue followupExecutorDlq() {
        return dlq(RabbitTopics.FOLLOWUP_EXECUTOR_DLQ);
    }

    // -----------------------------------------------------------------------
    // Bindings
    // -----------------------------------------------------------------------

    @Bean
    public Binding inboundBinding(TopicExchange whatsappExchange, Queue whatsappInboundQueue) {
        return BindingBuilder.bind(whatsappInboundQueue).to(whatsappExchange).with(RabbitTopics.ROUTING_INBOUND);
    }

    @Bean
    public Binding autoAiBinding(TopicExchange whatsappExchange, Queue whatsappAutoAiQueue) {
        return BindingBuilder.bind(whatsappAutoAiQueue).to(whatsappExchange).with(RabbitTopics.ROUTING_AUTO_AI);
    }

    @Bean
    public Binding senderBinding(TopicExchange whatsappExchange, Queue whatsappSenderQueue) {
        return BindingBuilder.bind(whatsappSenderQueue).to(whatsappExchange).with(RabbitTopics.ROUTING_SENDER);
    }

    @Bean
    public Binding followupBinding(TopicExchange whatsappExchange, Queue followupExecutorQueue) {
        return BindingBuilder.bind(followupExecutorQueue).to(whatsappExchange).with(RabbitTopics.ROUTING_FOLLOWUP_EXECUTOR);
    }

    @Bean
    public Binding inboundDlqBinding(DirectExchange whatsappDeadLetterExchange, Queue whatsappInboundDlq) {
        return BindingBuilder.bind(whatsappInboundDlq).to(whatsappDeadLetterExchange).with(RabbitTopics.INBOUND_QUEUE);
    }

    @Bean
    public Binding autoAiDlqBinding(DirectExchange whatsappDeadLetterExchange, Queue whatsappAutoAiDlq) {
        return BindingBuilder.bind(whatsappAutoAiDlq).to(whatsappDeadLetterExchange).with(RabbitTopics.AUTO_AI_QUEUE);
    }

    @Bean
    public Binding senderDlqBinding(DirectExchange whatsappDeadLetterExchange, Queue whatsappSenderDlq) {
        return BindingBuilder.bind(whatsappSenderDlq).to(whatsappDeadLetterExchange).with(RabbitTopics.SENDER_QUEUE);
    }

    @Bean
    public Binding followupDlqBinding(DirectExchange whatsappDeadLetterExchange, Queue followupExecutorDlq) {
        return BindingBuilder.bind(followupExecutorDlq).to(whatsappDeadLetterExchange).with(RabbitTopics.FOLLOWUP_EXECUTOR_QUEUE);
    }

    private static Queue primaryQueue(String name) {
        return QueueBuilder.durable(name)
                .deadLetterExchange(RabbitTopics.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(name)
                .build();
    }

    private static Queue dlq(String name) {
        return QueueBuilder.durable(name).build();
    }
}