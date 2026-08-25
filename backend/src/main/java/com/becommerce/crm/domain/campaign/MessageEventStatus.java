package com.becommerce.crm.domain.campaign;

/**
 * Status do evento por destinatário ({@code campaign_message_events}, V059).
 * DELIVERED/READ/RESPONDED/OPTED_OUT são atualizados por webhooks do provider
 * e alimentam o Analytics futuro (Sprint 19).
 */
public enum MessageEventStatus {
    PENDING,
    SENT,
    FAILED,
    CANCELLED,
    DELIVERED,
    READ,
    RESPONDED,
    OPTED_OUT
}
