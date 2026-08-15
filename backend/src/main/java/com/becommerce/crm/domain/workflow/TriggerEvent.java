package com.becommerce.crm.domain.workflow;

/**
 * Eventos (triggers) suportados pelo mecanismo de workflow (Sprint 14).
 * Lista propositalmente enxuta (Item 2): somente eventos já existentes no CRM
 * e com benefício real. Cada trigger possui um resolvedor de contexto para
 * avaliação de condições.
 */
public enum TriggerEvent {
    OPPORTUNITY_CREATED,
    OPPORTUNITY_STAGE_CHANGED,
    OPPORTUNITY_WON,
    OPPORTUNITY_LOST,
    OPPORTUNITY_STALE,
    TASK_CREATED,
    TASK_COMPLETED,
    ACTIVITY_CREATED,
    /** Mensagem de WhatsApp recebida (Sprint 16, FASE 14). */
    WHATSAPP_MESSAGE_RECEIVED
}
