package com.becommerce.crm.domain.ai;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Acao de escrita proposta pelo assistente de IA (AI-05). Correspondente a
 * tabela {@code ai_actions} (V051), protegida por RLS. Guarda a proposta
 * (tool, parametros tipados, descricao amigavel), o autor (empresa + usuario) e
 * a conversa de origem. A escrita REAL so ocorre apos confirmacao explicita do
 * usuario que a propoe; a confirmacao executa os parametros PERSISTIDOS - nunca
 * reexecuta a partir de argumentos do LLM.
 *
 * <p>Transicoes de estado: {@code PROPOSED} -> {@code EXECUTING} ->
 * {@code EXECUTED} | {@code FAILED}, ou {@code PROPOSED} -> {@code CANCELLED}.
 * Estados terminais sao idempotentes.</p>
 */
public class AiAction {

    private final UUID id;
    private final UUID companyId;
    private final UUID userId;
    private final UUID conversationId;
    private final String tool;
    private final String entityType;
    private final UUID entityId;
    private final Map<String, Object> parameters;
    private final String description;
    private AiActionStatus status;
    private Object result;
    private String errorMessage;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    private AiAction(UUID id, UUID companyId, UUID userId, UUID conversationId, String tool,
                     String entityType, UUID entityId, Map<String, Object> parameters, String description,
                     AiActionStatus status, Object result, String errorMessage,
                     LocalDateTime createdAt, LocalDateTime updatedAt, Long version) {
        this.id = id;
        this.companyId = companyId;
        this.userId = userId;
        this.conversationId = conversationId;
        this.tool = tool;
        this.entityType = entityType;
        this.entityId = entityId;
        this.parameters = parameters;
        this.description = description;
        this.status = status;
        this.result = result;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static AiAction propose(UUID companyId, UUID userId, UUID conversationId, String tool,
                                   String entityType, UUID entityId, Map<String, Object> parameters,
                                   String description) {
        LocalDateTime now = LocalDateTime.now();
        return new AiAction(UUID.randomUUID(), companyId, userId, conversationId, tool, entityType,
                entityId, parameters, description, AiActionStatus.PROPOSED, null, null, now, now, null);
    }

    public static AiAction reconstitute(UUID id, UUID companyId, UUID userId, UUID conversationId,
                                        String tool, String entityType, UUID entityId,
                                        Map<String, Object> parameters, String description,
                                        AiActionStatus status, Object result, String errorMessage,
                                        LocalDateTime createdAt, LocalDateTime updatedAt, Long version) {
        return new AiAction(id, companyId, userId, conversationId, tool, entityType, entityId,
                parameters, description, status, result, errorMessage, createdAt, updatedAt, version);
    }

    public void markExecuting() {
        this.status = AiActionStatus.EXECUTING;
        this.updatedAt = LocalDateTime.now();
    }

    public void markExecuted(Object result) {
        this.status = AiActionStatus.EXECUTED;
        this.result = result;
        this.errorMessage = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.status = AiActionStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = AiActionStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getUserId() { return userId; }
    public UUID getConversationId() { return conversationId; }
    public String getTool() { return tool; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public Map<String, Object> getParameters() { return parameters; }
    public String getDescription() { return description; }
    public AiActionStatus getStatus() { return status; }
    public Object getResult() { return result; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}