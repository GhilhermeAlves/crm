package com.becommerce.crm.domain.ai;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Trilha de auditoria de toda ação (invocação de ferramenta) executada pelo
 * agente de IA (WhatsApp AI Agent — Fase 1). Escopada por {@code companyId},
 * protegida por RLS. Registro imutável: nenhuma decisão do agente é
 * hardcoded — tudo que é executado fica auditado com input/output em JSON.
 */
public class AgentActionAudit {

    private static final String DEFAULT_EXECUTED_BY = "ai-agent";

    private final UUID id;
    private final UUID companyId;
    private final UUID conversationId;
    private final String toolName;
    private final String input;
    private final String result;
    private final String executedBy;
    private final LocalDateTime executedAt;
    private final String outcome;

    private AgentActionAudit(UUID id, UUID companyId, UUID conversationId, String toolName,
                             String input, String result, String executedBy,
                             LocalDateTime executedAt, String outcome) {
        this.id = id;
        this.companyId = companyId;
        this.conversationId = conversationId;
        this.toolName = toolName;
        this.input = input;
        this.result = result;
        this.executedBy = executedBy;
        this.executedAt = executedAt;
        this.outcome = outcome;
    }

    /** Cria um registro de auditoria para uma execução do agente autônomo ({@code executedBy = "ai-agent"}). */
    public static AgentActionAudit create(UUID companyId, UUID conversationId, String toolName,
                                          String input, String result, String outcome) {
        return new AgentActionAudit(UUID.randomUUID(), companyId, conversationId, toolName,
                input, result, DEFAULT_EXECUTED_BY, LocalDateTime.now(), outcome);
    }

    /** Cria um registro de auditoria informando explicitamente quem executou (ex.: email de usuário humano). */
    public static AgentActionAudit create(UUID companyId, UUID conversationId, String toolName,
                                          String input, String result, String executedBy,
                                          String outcome) {
        return new AgentActionAudit(UUID.randomUUID(), companyId, conversationId, toolName,
                input, result, executedBy, LocalDateTime.now(), outcome);
    }

    public static AgentActionAudit reconstitute(UUID id, UUID companyId, UUID conversationId,
                                                String toolName, String input, String result,
                                                String executedBy, LocalDateTime executedAt,
                                                String outcome) {
        return new AgentActionAudit(id, companyId, conversationId, toolName, input, result,
                executedBy, executedAt, outcome);
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getConversationId() { return conversationId; }
    public String getToolName() { return toolName; }
    public String getInput() { return input; }
    public String getResult() { return result; }
    public String getExecutedBy() { return executedBy; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public String getOutcome() { return outcome; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgentActionAudit other)) return false;
        return Objects.equals(id, other.id)
                && Objects.equals(companyId, other.companyId)
                && Objects.equals(conversationId, other.conversationId)
                && Objects.equals(toolName, other.toolName)
                && Objects.equals(input, other.input)
                && Objects.equals(result, other.result)
                && Objects.equals(executedBy, other.executedBy)
                && Objects.equals(outcome, other.outcome);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, companyId, conversationId, toolName, input, result,
                executedBy, outcome);
    }
}
