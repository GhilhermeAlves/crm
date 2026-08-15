package com.becommerce.crm.domain.omnichannel;

/**
 * Status da mensagem, refletindo o ciclo de vida do provedor.
 * Só os estados necessários (FASE 8/10 do Sprint 16).
 */
public enum MessageStatus {
    /** Mensagem criada no CRM, ainda não confirmada pelo provedor. */
    PENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}
