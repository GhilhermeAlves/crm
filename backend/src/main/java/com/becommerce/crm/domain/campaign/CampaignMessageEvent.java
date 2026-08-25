package com.becommerce.crm.domain.campaign;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento por destinatário (Sprint 17), tabela {@code campaign_message_events} (V059).
 * A constraint UNIQUE (execution_id, recipient_id) no banco garante idempotência
 * do envio mesmo sob retry/concorrência/restart.
 */
public class CampaignMessageEvent {

    public static final int MAX_ATTEMPTS = 3;

    private final UUID id;
    private final UUID companyId;
    private final UUID executionId;
    private final UUID campaignId;
    private final UUID recipientId;
    private final String recipientType;
    private final String recipientPhone;
    private MessageEventStatus status;
    private int attempts;
    private String errorReason;
    private String providerMessageId;
    private LocalDateTime occurredAt;
    private final LocalDateTime createdAt;

    private CampaignMessageEvent(UUID id, UUID companyId, UUID executionId, UUID campaignId,
                                 UUID recipientId, String recipientType, String recipientPhone,
                                 MessageEventStatus status, int attempts, String errorReason,
                                 String providerMessageId, LocalDateTime occurredAt,
                                 LocalDateTime createdAt) {
        this.id = id;
        this.companyId = companyId;
        this.executionId = executionId;
        this.campaignId = campaignId;
        this.recipientId = recipientId;
        this.recipientType = recipientType;
        this.recipientPhone = recipientPhone;
        this.status = status;
        this.attempts = attempts;
        this.errorReason = errorReason;
        this.providerMessageId = providerMessageId;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
    }

    public static CampaignMessageEvent createPending(UUID companyId, UUID executionId,
                                                     UUID campaignId, UUID recipientId,
                                                     String recipientType, String recipientPhone) {
        return new CampaignMessageEvent(UUID.randomUUID(), companyId, executionId, campaignId,
                recipientId, recipientType != null ? recipientType : "CONTACT",
                recipientPhone, MessageEventStatus.PENDING, 0, null, null, null,
                LocalDateTime.now());
    }

    public static CampaignMessageEvent reconstitute(UUID id, UUID companyId, UUID executionId,
                                                    UUID campaignId, UUID recipientId,
                                                    String recipientType, String recipientPhone,
                                                    MessageEventStatus status, int attempts,
                                                    String errorReason, String providerMessageId,
                                                    LocalDateTime occurredAt, LocalDateTime createdAt) {
        return new CampaignMessageEvent(id, companyId, executionId, campaignId, recipientId,
                recipientType, recipientPhone, status, attempts, errorReason, providerMessageId,
                occurredAt, createdAt);
    }

    public void markSent(String providerMessageId) {
        this.status = MessageEventStatus.SENT;
        this.providerMessageId = providerMessageId;
        this.errorReason = null;
        this.occurredAt = LocalDateTime.now();
    }

    public void markFailed(String errorReason) {
        this.status = MessageEventStatus.FAILED;
        this.errorReason = errorReason;
        this.occurredAt = LocalDateTime.now();
    }

    public void recordAttempt() {
        this.attempts++;
    }

    /** Volta a PENDING para nova tentativa enquanto houver orçamento de tentativas. */
    public void backToPending(String lastError) {
        if (!canRetry()) {
            throw new IllegalStateException("Evento excedeu o número máximo de tentativas.");
        }
        this.status = MessageEventStatus.PENDING;
        this.errorReason = lastError;
    }

    public boolean canRetry() {
        return attempts < MAX_ATTEMPTS;
    }

    public void prepareRetry() {
        if (!canRetry()) {
            throw new IllegalStateException("Evento excedeu o número máximo de tentativas.");
        }
        this.status = MessageEventStatus.PENDING;
    }

    public void cancel() {
        this.status = MessageEventStatus.CANCELLED;
        this.occurredAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getExecutionId() { return executionId; }
    public UUID getCampaignId() { return campaignId; }
    public UUID getRecipientId() { return recipientId; }
    public String getRecipientType() { return recipientType; }
    public String getRecipientPhone() { return recipientPhone; }
    public MessageEventStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public String getErrorReason() { return errorReason; }
    public String getProviderMessageId() { return providerMessageId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
