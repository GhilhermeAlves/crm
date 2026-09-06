package com.becommerce.crm.domain.omnichannel;

/**
 * Modo de atendimento de uma conversa (Sprint 3 - Human Takeover).
 *
 * <ul>
 *   <li>{@link #AUTOMATIC}: o agente de IA autônomo pode responder (padrão);</li>
 *   <li>{@link #HUMAN}: um humano assumiu — a IA autônoma fica suspensa até
 *       {@code release}.</li>
 * </ul>
 */
public enum ConversationMode {
    AUTOMATIC,
    HUMAN
}