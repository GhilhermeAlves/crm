package com.becommerce.crm.infrastructure.rabbit;

import com.becommerce.crm.application.omnichannel.event.WhatsAppSendEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Transporte MQ REAL (Testcontainers RabbitMQ) — Sprint 23. Prova com broker de
 * verdade que os componentes de INFRAESTRUTURA que adicionamos funcionam:
 * <ol>
 *   <li>a topologia ({@link WhatsAppRabbitTopology}) é declarada no broker
 *       (exchanges, filas + DLQs, bindings e dead-letter);</li>
 *   <li>o converter JSON tipado ({@link RabbitConfig}) faz round-trip dos eventos
 *       (o record chega no consumer com o {@code __TypeId__} correto);</li>
 *   <li>um {@code @RabbitListener} na fila {@code crm.whatsapp.sender} recebe o
 *       {@link WhatsAppSendEvent} publicado via {@code RabbitTemplate} com a mesma
 *       exchange/routing key da porta {@code WhatsAppEventPublisher}.
 * </ol>
 *
 * <p>Testa apenas a INFRAESTRUTURA (sem domínio/JPA): os processadores lógicos
 * são cobertos por unit tests; aqui garantimos que o canal de transporte
 * funciona de ponta a ponta (mesmo padrão dos ITs de isolamento das sprints).
 */
@Testcontainers
class RabbitMessagingFlowIT {

    @Container
    static final RabbitMQContainer RABBIT =
            new RabbitMQContainer("rabbitmq:3.12-management");

    @Test
    void topology_shouldBeDeclaredOnRealBroker() {
        CachingConnectionFactory cf = connectionFactory();
        try {
            RabbitAdmin admin = new RabbitAdmin(cf);
            WhatsAppRabbitTopology topology = new WhatsAppRabbitTopology();

            Exchange exchange = topology.whatsappExchange();
            admin.declareExchange(exchange);
            admin.declareExchange(topology.whatsappDeadLetterExchange());
            for (Queue queue : queues(topology)) {
                assertNotNull(admin.declareQueue(queue), "Fila deve ser declarada: " + queue.getName());
            }
            for (Binding binding : bindings(topology)) {
                admin.declareBinding(binding);
            }

            // O payload publicado no tópico com routing `whatsapp.sender` deve chegar
            // APENAS na fila `crm.whatsapp.sender` (não nas irmãs).
            for (String queueName : List.of(RabbitTopics.INBOUND_QUEUE, RabbitTopics.AUTO_AI_QUEUE,
                    RabbitTopics.SENDER_QUEUE, RabbitTopics.FOLLOWUP_EXECUTOR_QUEUE)) {
                long count = admin.getRabbitTemplate().execute(channel ->
                        channel.messageCount(queueName));
                assertEquals(0L, count, "Fila vazia no início: " + queueName);
            }
            assertTrue(queueExists(admin, RabbitTopics.SENDER_QUEUE));
            assertTrue(queueExists(admin, RabbitTopics.SENDER_DLQ));
        } finally {
            cf.destroy();
        }
    }

    @Test
    void senderQueue_shouldRoundTripWhatsAppSendEvent() throws Exception {
        CachingConnectionFactory cf = connectionFactory();
        try {
            RabbitAdmin admin = new RabbitAdmin(cf);
            WhatsAppRabbitTopology topology = new WhatsAppRabbitTopology();
            admin.declareExchange(topology.whatsappExchange());
            admin.declareExchange(topology.whatsappDeadLetterExchange());
            for (Queue queue : queues(topology)) {
                admin.declareQueue(queue);
            }
            for (Binding binding : bindings(topology)) {
                admin.declareBinding(binding);
            }

            MessageConverter converter = new RabbitConfig().messageConverter(rabbitCompatibleObjectMapper());
            RabbitTemplate template = new RabbitTemplate(cf);
            template.setMessageConverter(converter);

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<WhatsAppSendEvent> received = new AtomicReference<>();
            SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(cf);
            MessageListenerAdapter adapter =
                    new MessageListenerAdapter(new Consumer(received, latch), "handleSend");
            adapter.setMessageConverter(converter);
            container.setQueueNames(RabbitTopics.SENDER_QUEUE);
            container.setMessageListener(adapter);
            container.start();

            try {
                WhatsAppSendEvent published = WhatsAppSendEvent.ofFollowUp(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        UUID.randomUUID(), "+5511999998888", "Olá via fila!", UUID.randomUUID());
                template.convertAndSend(RabbitTopics.WHATSAPP_EXCHANGE, RabbitTopics.ROUTING_SENDER, published);

                assertTrue(latch.await(30, TimeUnit.SECONDS), "Mensagem deve ser consumida em até 30s");
                WhatsAppSendEvent event = received.get();
                assertNotNull(event);
                assertEquals("Olá via fila!", event.body());
                assertEquals("+5511999998888", event.to());
                assertNotNull(event.eventId());
                assertNotNull(event.occurredAt());
                // `followUpId` opcional (null) deve sobreviver ao round-trip.
                assertNotNull(event.followUpId());

                // Nenhum vazamento nas filas irmãs / DLQ.
                long inboundCount = admin.getRabbitTemplate().execute(channel ->
                        channel.messageCount(RabbitTopics.INBOUND_QUEUE));
                assertEquals(0L, inboundCount);
                long senderDlqCount = admin.getRabbitTemplate().execute(channel ->
                        channel.messageCount(RabbitTopics.SENDER_DLQ));
                assertEquals(0L, senderDlqCount);
            } finally {
                container.stop();
            }
        } finally {
            cf.destroy();
        }
    }

    @Test
    void dlq_shouldReceivePoisonedMessageAfterRejected() throws Exception {
        CachingConnectionFactory cf = connectionFactory();
        try {
            RabbitAdmin admin = new RabbitAdmin(cf);
            WhatsAppRabbitTopology topology = new WhatsAppRabbitTopology();
            admin.declareExchange(topology.whatsappExchange());
            admin.declareExchange(topology.whatsappDeadLetterExchange());
            for (Queue queue : queues(topology)) {
                admin.declareQueue(queue);
            }
            for (Binding binding : bindings(topology)) {
                admin.declareBinding(binding);
            }

            MessageConverter converter = new RabbitConfig().messageConverter(rabbitCompatibleObjectMapper());
            RabbitTemplate template = new RabbitTemplate(cf);
            template.setMessageConverter(converter);

            CountDownLatch rejected = new CountDownLatch(1);
            SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(cf);
            container.setQueueNames(RabbitTopics.AUTO_AI_QUEUE);
            container.setAcknowledgeMode(AcknowledgeMode.AUTO);
            container.setDefaultRequeueRejected(false);
            container.setMessageListener(message -> {
                // Sempre rejeita: vai para a DLQ (default-requeue-rejected=false) e o
                // dead-letter exchange a roteia para `crm.whatsapp.auto-ai.dlq`.
                rejected.countDown();
                throw new RuntimeException("poisoned");
            });
            container.start();

            try {
                template.convertAndSend(RabbitTopics.WHATSAPP_EXCHANGE, RabbitTopics.ROUTING_AUTO_AI,
                        "payload-any");

                assertTrue(rejected.await(30, TimeUnit.SECONDS), "Mensagem deve chegar ao listener");
                // Aguarda o repúdio/roteamento para a DLQ (assíncrono no broker).
                long deadline = System.currentTimeMillis() + 15_000;
                long dlqCount = 0;
                while (System.currentTimeMillis() < deadline) {
                    dlqCount = admin.getRabbitTemplate().execute(channel ->
                            channel.messageCount(RabbitTopics.AUTO_AI_DLQ));
                    if (dlqCount > 0) {
                        break;
                    }
                    Thread.sleep(250);
                }
                assertEquals(1L, dlqCount, "Mensagem rejeitada deve parar na DLQ (não ser perdida)");
            } finally {
                container.stop();
            }
        } finally {
            cf.destroy();
        }
    }

    // ------------------------------------------------------------------- helpers

    public static class Consumer {
        private final AtomicReference<WhatsAppSendEvent> sink;
        private final CountDownLatch latch;

        Consumer(AtomicReference<WhatsAppSendEvent> sink, CountDownLatch latch) {
            this.sink = sink;
            this.latch = latch;
        }

        public void handleSend(WhatsAppSendEvent event) {
            sink.set(event);
            latch.countDown();
        }
    }

    private CachingConnectionFactory connectionFactory() {
        CachingConnectionFactory cf = new CachingConnectionFactory(RABBIT.getHost(), RABBIT.getAmqpPort());
        cf.setUsername("guest");
        cf.setPassword("guest");
        return cf;
    }

    private boolean queueExists(RabbitAdmin admin, String name) {
        try {
            return admin.getRabbitTemplate().execute(channel -> channel.queueDeclarePassive(name)) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private List<Queue> queues(WhatsAppRabbitTopology t) {
        return List.of(t.whatsappInboundQueue(), t.whatsappAutoAiQueue(), t.whatsappSenderQueue(),
                t.followupExecutorQueue(), t.whatsappInboundDlq(), t.whatsappAutoAiDlq(),
                t.whatsappSenderDlq(), t.followupExecutorDlq());
    }

    private List<Binding> bindings(WhatsAppRabbitTopology t) {
        return List.of(t.inboundBinding(t.whatsappExchange(), t.whatsappInboundQueue()),
                t.autoAiBinding(t.whatsappExchange(), t.whatsappAutoAiQueue()),
                t.senderBinding(t.whatsappExchange(), t.whatsappSenderQueue()),
                t.followupBinding(t.whatsappExchange(), t.followupExecutorQueue()),
                t.inboundDlqBinding(t.whatsappDeadLetterExchange(), t.whatsappInboundDlq()),
                t.autoAiDlqBinding(t.whatsappDeadLetterExchange(), t.whatsappAutoAiDlq()),
                t.senderDlqBinding(t.whatsappDeadLetterExchange(), t.whatsappSenderDlq()),
                t.followupDlqBinding(t.whatsappDeadLetterExchange(), t.followupExecutorDlq()));
    }

    /** ObjectMapper equivalente ao do Spring Boot (JavaTime + timestamps desativados). */
    private ObjectMapper rabbitCompatibleObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
    }
}