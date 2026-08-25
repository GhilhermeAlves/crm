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
    WHATSAPP_MESSAGE_RECEIVED,
    /** Contato criado (Sprint 18 — Automações Omnichannel). */
    CONTACT_CREATED,
    /** Status de lead alterado (Sprint 18). */
    LEAD_STATUS_CHANGED,
    /** Execução de campanha concluída (Sprint 18). */
    CAMPAIGN_COMPLETED
}
