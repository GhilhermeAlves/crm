package com.becommerce.crm.domain.ai;

/**
 * Resultado da execução de uma ferramenta pelo agente de IA (WhatsApp AI
 * Agent — Fase 1). DTO imutável: não é uma entidade persistida (o registro
 * durável correspondente é o {@link AgentActionAudit}).
 */
public final class ToolExecutionResult {

    private final boolean success;
    private final String content;
    private final String errorMessage;
    private final long executionTimeMs;

    private ToolExecutionResult(boolean success, String content, String errorMessage,
                                long executionTimeMs) {
        this.success = success;
        this.content = content;
        this.errorMessage = errorMessage;
        this.executionTimeMs = executionTimeMs;
    }

    /** Resultado de sucesso: {@code content} traz o payload (JSON ou texto) da ferramenta. */
    public static ToolExecutionResult success(String content, long executionTimeMs) {
        return new ToolExecutionResult(true, content, null, executionTimeMs);
    }

    /** Resultado de falha: {@code errorMessage} descreve o motivo (não expõe stack trace ao usuário). */
    public static ToolExecutionResult failure(String errorMessage, long executionTimeMs) {
        return new ToolExecutionResult(false, null, errorMessage, executionTimeMs);
    }

    public boolean isSuccess() { return success; }
    public String getContent() { return content; }
    public String getErrorMessage() { return errorMessage; }
    public long getExecutionTimeMs() { return executionTimeMs; }
}
