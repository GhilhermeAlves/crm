package com.becommerce.crm.domain.followup;

import com.becommerce.crm.domain.followup.exception.FollowUpValidationException;

/**
 * FollowUp — ação futura associada a uma conversa omnichannel (Sprint 22).
 *
 * <p>Entidade de domínio pura (POJO, sem anotações de infraestrutura), no
 * padrão de {@code Conversation}/{@code Activity}: factories estáticas
 * {@code create}/{@code reconstitute} + métodos de mutação que validam a
 * máquina de estados (ver {@link FollowUpStatus}).
 *
 * <p>O {@code idempotencyKey} permite criação idempotente (o serviço devolve o
 * follow-up já existente quando a mesma chave é reenviada). A idempotência de
 * EXECUÇÃO é garantida pelo claim atômico no repositório
 * ({@code PENDING -> PROCESSING} com guard de status).
 */
public class FollowUp {

    /** Limite de tentativas do worker antes de marcar FAILED (retry seguro). */
    public static final int MAX_ATTEMPTS = 3;

    private final java.util.UUID id;
    private final java.util.UUID companyId;
    private final java.util.UUID conversationId;
    private FollowUpStatus status;
    private final FollowUpAction actionType;
    private String actionContent;
    private java.time.LocalDateTime executeAt;
    private int attempts;
    private String lastError;
    private String resultText;
    private java.time.LocalDateTime processingStartedAt;
    private java.time.LocalDateTime processedAt;
    private java.time.LocalDateTime cancelledAt;
    private FollowUpCancellationReason cancelledReason;
    private final java.util.UUID idempotencyKey;
    private final java.util.UUID sequenceId;
    private final java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    private FollowUp(java.util.UUID id, java.util.UUID companyId, java.util.UUID conversationId,
                     FollowUpStatus status, FollowUpAction actionType, String actionContent,
                     java.time.LocalDateTime executeAt, int attempts, String lastError, String resultText,
                     java.time.LocalDateTime processingStartedAt, java.time.LocalDateTime processedAt,
                     java.time.LocalDateTime cancelledAt, FollowUpCancellationReason cancelledReason,
                     java.util.UUID idempotencyKey, java.util.UUID sequenceId,
                     java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.conversationId = conversationId;
        this.status = status;
        this.actionType = actionType;
        this.actionContent = actionContent;
        this.executeAt = executeAt;
        this.attempts = attempts;
        this.lastError = lastError;
        this.resultText = resultText;
        this.processingStartedAt = processingStartedAt;
        this.processedAt = processedAt;
        this.cancelledAt = cancelledAt;
        this.cancelledReason = cancelledReason;
        this.idempotencyKey = idempotencyKey;
        this.sequenceId = sequenceId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static FollowUp create(java.util.UUID companyId, java.util.UUID conversationId,
                                  FollowUpAction actionType, String actionContent,
                                  java.time.LocalDateTime executeAt, java.util.UUID idempotencyKey) {
        return create(companyId, conversationId, actionType, actionContent, executeAt, idempotencyKey, null);
    }

    public static FollowUp create(java.util.UUID companyId, java.util.UUID conversationId,
                                  FollowUpAction actionType, String actionContent,
                                  java.time.LocalDateTime executeAt, java.util.UUID idempotencyKey,
                                  java.util.UUID sequenceId) {
        validateAction(actionType, actionContent);
        if (executeAt == null) {
            throw new FollowUpValidationException("A data/hora de execução é obrigatória");
        }
        return new FollowUp(java.util.UUID.randomUUID(), companyId, conversationId, FollowUpStatus.PENDING,
                actionType, actionContent, executeAt, 0, null, null, null, null, null, null,
                idempotencyKey, sequenceId, java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
    }

    public static FollowUp reconstitute(java.util.UUID id, java.util.UUID companyId, java.util.UUID conversationId,
                                        FollowUpStatus status, FollowUpAction actionType, String actionContent,
                                        java.time.LocalDateTime executeAt, int attempts, String lastError,
                                        String resultText, java.time.LocalDateTime processingStartedAt,
                                        java.time.LocalDateTime processedAt, java.time.LocalDateTime cancelledAt,
                                        FollowUpCancellationReason cancelledReason, java.util.UUID idempotencyKey,
                                        java.util.UUID sequenceId,
                                        java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {
        return new FollowUp(id, companyId, conversationId, status, actionType, actionContent, executeAt,
                attempts, lastError, resultText, processingStartedAt, processedAt, cancelledAt,
                cancelledReason, idempotencyKey, sequenceId, createdAt, updatedAt);
    }

    /** Cancelamento pelo usuário: apenas a partir de PENDING. */
    public void cancel() {
        if (status != FollowUpStatus.PENDING) {
            throw new FollowUpValidationException(
                    "Não é possível cancelar um follow-up " + status.name() + " (apenas PENDING)");
        }
        this.status = FollowUpStatus.CANCELLED;
        this.cancelledReason = FollowUpCancellationReason.USER;
        this.cancelledAt = java.time.LocalDateTime.now();
        this.updatedAt = java.time.LocalDateTime.now();
    }

    /** Movimenta PENDING -> PROCESSING no domínio (repositório faz o claim atômico). */
    public void markProcessing() {
        requireTransition(FollowUpStatus.PENDING);
        this.status = FollowUpStatus.PROCESSING;
        this.processingStartedAt = java.time.LocalDateTime.now();
        this.updatedAt = java.time.LocalDateTime.now();
    }

    /** Marca o follow-up como executado (SENT). */
    public void markSent(String messageId) {
        requireTransition(FollowUpStatus.PROCESSING);
        this.status = FollowUpStatus.SENT;
        this.resultText = messageId;
        this.processedAt = java.time.LocalDateTime.now();
        this.updatedAt = java.time.LocalDateTime.now();
    }

    /** Falha terminal (após {@value #MAX_ATTEMPTS} tentativas). */
    public void markFailed(String error) {
        requireTransition(FollowUpStatus.PROCESSING);
        this.status = FollowUpStatus.FAILED;
        this.lastError = error;
        this.processedAt = java.time.LocalDateTime.now();
        this.updatedAt = java.time.LocalDateTime.now();
    }

    /** Retry seguro: PROCESSING -> PENDING reagendado com backoff. */
    public void scheduleRetry(String error, java.time.LocalDateTime nextExecuteAt) {
        requireTransition(FollowUpStatus.PROCESSING);
        if (attempts >= MAX_ATTEMPTS) {
            throw new FollowUpValidationException("Número máximo de tentativas atingido (" + MAX_ATTEMPTS + ")");
        }
        this.status = FollowUpStatus.PENDING;
        this.attempts++;
        this.lastError = error;
        this.executeAt = nextExecuteAt;
        this.processingStartedAt = null;
        this.updatedAt = java.time.LocalDateTime.now();
    }

    /** Cancelamento por regra do processador (modo HUMAN ou follow-up obsoleto). */
    public void markCancelledByRule(FollowUpCancellationReason reason) {
        if (status != FollowUpStatus.PENDING && status != FollowUpStatus.PROCESSING) {
            throw new FollowUpValidationException(
                    "Não é possível cancelar um follow-up " + status.name() + " (apenas PENDING/PROCESSING)");
        }
        this.status = FollowUpStatus.CANCELLED;
        this.cancelledReason = reason;
        this.cancelledAt = java.time.LocalDateTime.now();
        this.updatedAt = java.time.LocalDateTime.now();
    }

    private void requireTransition(FollowUpStatus expected) {
        if (this.status != expected) {
            throw new FollowUpValidationException(
                    "Transição inválida de follow-up: " + this.status.name() + " -> " + expected.name());
        }
    }

    private static void validateAction(FollowUpAction actionType, String actionContent) {
        if (actionType == null) {
            throw new FollowUpValidationException("O tipo de ação é obrigatório");
        }
        if (actionContent == null || actionContent.isBlank()) {
            throw new FollowUpValidationException("O conteúdo da ação é obrigatório");
        }
        if (actionContent.length() > 4000) {
            throw new FollowUpValidationException("O conteúdo da ação excede o limite de 4000 caracteres");
        }
    }

    public java.util.UUID getId() { return id; }
    public java.util.UUID getCompanyId() { return companyId; }
    public java.util.UUID getConversationId() { return conversationId; }
    public FollowUpStatus getStatus() { return status; }
    public FollowUpAction getActionType() { return actionType; }
    public String getActionContent() { return actionContent; }
    public java.time.LocalDateTime getExecuteAt() { return executeAt; }
    public int getAttempts() { return attempts; }
    public String getLastError() { return lastError; }
    public String getResultText() { return resultText; }
    public java.time.LocalDateTime getProcessingStartedAt() { return processingStartedAt; }
    public java.time.LocalDateTime getProcessedAt() { return processedAt; }
    public java.time.LocalDateTime getCancelledAt() { return cancelledAt; }
    public FollowUpCancellationReason getCancelledReason() { return cancelledReason; }
    public java.util.UUID getIdempotencyKey() { return idempotencyKey; }
    public java.util.UUID getSequenceId() { return sequenceId; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }

    public boolean isPending() { return status == FollowUpStatus.PENDING; }
    public boolean isProcessing() { return status == FollowUpStatus.PROCESSING; }
}