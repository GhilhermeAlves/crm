package com.becommerce.crm.domain.workflow.event;

import com.becommerce.crm.domain.workflow.TriggerEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Evento de domínio que dispara a avaliação de workflows (Sprint 14).
 *
 * <p>É publicado através do {@code EventPublisher} existente nos pontos em que
 * um "algo aconteceu" comercial relevante (opportunity/task/activity). Mantém os
 * identificadores da entidade origem para que as ações possam vincular as novas
 * entidades criadas (ex.: Task vinculada à Opportunity). {@code context()} expõe
 * os campos resolvidos usados pela avaliação de condições.
 *
 * <p>{@code eventId} é a chave de idempotência junto com a ação (Item 6): a
 * execução de {@code (eventId, actionId)} acontece no máximo uma vez.
 */
public record WorkflowTriggerEvent(
        UUID eventId,
        UUID companyId,
        TriggerEvent trigger,
        UUID opportunityId,
        UUID contactId,
        UUID taskId,
        UUID activityId,
        String opportunityStage,
        BigDecimal opportunityValue,
        String taskPriority,
        String activityType,
        LocalDateTime occurredAt,
        Map<String, Object> context
) {

    public static WorkflowTriggerEvent opportunityCreated(UUID companyId, UUID opportunityId,
                                                          UUID contactId, String stageName, BigDecimal value) {
        Map<String, Object> ctx = ctx();
        ctx.put("opportunity.stage", stageName);
        ctx.put("opportunity.value", value);
        return build(TriggerEvent.OPPORTUNITY_CREATED, companyId, opportunityId, contactId, null, null,
                stageName, value, null, null, ctx);
    }

    public static WorkflowTriggerEvent opportunityStageChanged(UUID companyId, UUID opportunityId,
                                                               UUID contactId, String stageName, BigDecimal value) {
        Map<String, Object> ctx = ctx();
        ctx.put("opportunity.stage", stageName);
        ctx.put("opportunity.value", value);
        return build(TriggerEvent.OPPORTUNITY_STAGE_CHANGED, companyId, opportunityId, contactId, null, null,
                stageName, value, null, null, ctx);
    }

    public static WorkflowTriggerEvent opportunityWon(UUID companyId, UUID opportunityId,
                                                      UUID contactId, String stageName, BigDecimal value) {
        Map<String, Object> ctx = ctx();
        ctx.put("opportunity.stage", stageName);
        ctx.put("opportunity.value", value);
        return build(TriggerEvent.OPPORTUNITY_WON, companyId, opportunityId, contactId, null, null,
                stageName, value, null, null, ctx);
    }

    public static WorkflowTriggerEvent opportunityLost(UUID companyId, UUID opportunityId,
                                                       UUID contactId, String stageName, BigDecimal value) {
        Map<String, Object> ctx = ctx();
        ctx.put("opportunity.stage", stageName);
        ctx.put("opportunity.value", value);
        return build(TriggerEvent.OPPORTUNITY_LOST, companyId, opportunityId, contactId, null, null,
                stageName, value, null, null, ctx);
    }

    /**
     * Disparado por varredura periódica para oportunidades paradas (Item 11).
     *
     * <p>{@code eventId} é determinístico (= {@code opportunityId}) para que a
     * idempotência (Item 6) garanta que cada ação (ex.: criar tarefa de follow-up)
     * seja executada no máximo uma vez por oportunidade, mesmo com varreduras
     * diárias repetidas.
     */
    public static WorkflowTriggerEvent opportunityStale(UUID companyId, UUID opportunityId,
                                                        UUID contactId, String stageName,
                                                        BigDecimal value, long daysWithoutActivity) {
        Map<String, Object> ctx = ctx();
        ctx.put("opportunity.stage", stageName);
        ctx.put("opportunity.value", value);
        ctx.put("opportunity.daysWithoutActivity", daysWithoutActivity);
        return new WorkflowTriggerEvent(opportunityId, companyId, TriggerEvent.OPPORTUNITY_STALE,
                opportunityId, contactId, null, null, stageName, value, null, null,
                LocalDateTime.now(), ctx);
    }

    public static WorkflowTriggerEvent taskCreated(UUID companyId, UUID taskId, UUID contactId,
                                                   UUID opportunityId, String priority) {
        Map<String, Object> ctx = ctx();
        ctx.put("task.priority", priority);
        return build(TriggerEvent.TASK_CREATED, companyId, opportunityId, contactId, taskId, null,
                null, null, priority, null, ctx);
    }

    public static WorkflowTriggerEvent taskCompleted(UUID companyId, UUID taskId, UUID contactId,
                                                     UUID opportunityId, String priority) {
        Map<String, Object> ctx = ctx();
        ctx.put("task.priority", priority);
        return build(TriggerEvent.TASK_COMPLETED, companyId, opportunityId, contactId, taskId, null,
                null, null, priority, null, ctx);
    }

    public static WorkflowTriggerEvent activityCreated(UUID companyId, UUID activityId, UUID contactId,
                                                       UUID opportunityId, String type) {
        Map<String, Object> ctx = ctx();
        ctx.put("activity.type", type);
        return build(TriggerEvent.ACTIVITY_CREATED, companyId, null, contactId, null, activityId,
                null, null, null, type, ctx);
    }

    /**
     * Mensagem de WhatsApp recebida (Sprint 16, FASE 14). {@code eventId} é
     * determinístico (= {@code messageId}) para que cada ação seja executada no
     * máximo uma vez por mensagem.
     */
    public static WorkflowTriggerEvent whatsAppMessageReceived(UUID companyId, UUID contactId,
                                                               UUID conversationId, UUID messageId,
                                                               String from, String body) {
        Map<String, Object> ctx = ctx();
        ctx.put("whatsapp.from", from);
        ctx.put("whatsapp.body", body);
        ctx.put("whatsapp.conversationId", conversationId.toString());
        return new WorkflowTriggerEvent(messageId, companyId, TriggerEvent.WHATSAPP_MESSAGE_RECEIVED,
                null, contactId, null, null, null, null, null, null,
                LocalDateTime.now(), ctx);
    }

    /**
     * Contato criado (Sprint 18). Contexto: contact.email/phone.
     */
    public static WorkflowTriggerEvent contactCreated(UUID companyId, UUID contactId,
                                                      String email, String phone) {
        Map<String, Object> ctx = ctx();
        ctx.put("contact.email", email);
        ctx.put("contact.phone", phone);
        return new WorkflowTriggerEvent(UUID.randomUUID(), companyId,
                TriggerEvent.CONTACT_CREATED, null, contactId, null, null,
                null, null, null, null, LocalDateTime.now(), ctx);
    }

    /**
     * Status de lead alterado (Sprint 18). {@code eventId} é determinístico por
     * transição (leadId + status novo + timestamp de minuto) para idempotência.
     */
    public static WorkflowTriggerEvent leadStatusChanged(UUID companyId, UUID leadId,
                                                         UUID contactId, String previousStatus,
                                                         String newStatus) {
        Map<String, Object> ctx = ctx();
        ctx.put("lead.status", newStatus);
        ctx.put("lead.previousStatus", previousStatus);
        return new WorkflowTriggerEvent(
                UUID.nameUUIDFromBytes((leadId + ":" + newStatus + ":" + System.currentTimeMillis())
                        .getBytes()),
                companyId, TriggerEvent.LEAD_STATUS_CHANGED, null, contactId, null, null,
                null, null, null, null, LocalDateTime.now(), ctx);
    }

    /**
     * Execução de campanha concluída (Sprint 18). {@code eventId} determinístico
     * (= executionId) — cada execução dispara as ações no máximo uma vez.
     */
    public static WorkflowTriggerEvent campaignCompleted(UUID companyId, UUID campaignId,
                                                         UUID executionId, int failedCount,
                                                         int totalRecipients) {
        Map<String, Object> ctx = ctx();
        ctx.put("campaign.id", campaignId.toString());
        ctx.put("campaign.failedCount", failedCount);
        ctx.put("campaign.totalRecipients", totalRecipients);
        return new WorkflowTriggerEvent(executionId, companyId, TriggerEvent.CAMPAIGN_COMPLETED,
                null, null, null, null, null, null, null, null,
                LocalDateTime.now(), ctx);
    }

    private static Map<String, Object> ctx() {
        return new LinkedHashMap<>();
    }

    private static WorkflowTriggerEvent build(TriggerEvent trigger, UUID companyId, UUID opportunityId,
                                              UUID contactId, UUID taskId, UUID activityId, String stageName,
                                              BigDecimal value, String priority, String type,
                                              Map<String, Object> context) {
        return new WorkflowTriggerEvent(UUID.randomUUID(), companyId, trigger, opportunityId, contactId,
                taskId, activityId, stageName, value, priority, type, LocalDateTime.now(), context);
    }
}