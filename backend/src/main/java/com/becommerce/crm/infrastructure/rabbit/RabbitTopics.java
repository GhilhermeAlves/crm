package com.becommerce.crm.infrastructure.rabbit;

/** Constantes de roteamento do barramento RabbitMQ (Sprint 23). */
public final class RabbitTopics {

    /** Exchange tópico único do fluxo WhatsApp/UAZAPI. */
    public static final String WHATSAPP_EXCHANGE = "crm.whatsapp";

    /** Exchange DIRECT de dead-letter (DLQs de todas as filas do fluxo). */
    public static final String DEAD_LETTER_EXCHANGE = "crm.whatsapp.dlx";

    public static final String INBOUND_QUEUE = "crm.whatsapp.inbound";
    public static final String AUTO_AI_QUEUE = "crm.whatsapp.auto-ai";
    public static final String SENDER_QUEUE = "crm.whatsapp.sender";
    public static final String FOLLOWUP_EXECUTOR_QUEUE = "crm.followup.executor";

    public static final String INBOUND_DLQ = INBOUND_QUEUE + ".dlq";
    public static final String AUTO_AI_DLQ = AUTO_AI_QUEUE + ".dlq";
    public static final String SENDER_DLQ = SENDER_QUEUE + ".dlq";
    public static final String FOLLOWUP_EXECUTOR_DLQ = FOLLOWUP_EXECUTOR_QUEUE + ".dlq";

    public static final String ROUTING_INBOUND = "whatsapp.inbound";
    public static final String ROUTING_AUTO_AI = "whatsapp.auto-ai";
    public static final String ROUTING_SENDER = "whatsapp.sender";
    public static final String ROUTING_FOLLOWUP_EXECUTOR = "followup.executor";

    private RabbitTopics() {
    }
}