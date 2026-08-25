package com.becommerce.crm.domain.campaign;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Execução materializada de uma campanha (Sprint 17),
 * tabela {@code campaign_executions} (V058). Batch + cursor persistido.
 */
public class CampaignExecution {

    private final UUID id;
    private final UUID companyId;
    private final UUID campaignId;
    private ExecutionStatus status;
    private final String templateSnapshot;
    private int totalRecipients;
    private int processedCount;
    private int failedCount;
    private int cursorOffset;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private final LocalDateTime createdAt;

    private CampaignExecution(UUID id, UUID companyId, UUID campaignId, ExecutionStatus status,
                              String templateSnapshot, int totalRecipients, int processedCount,
                              int failedCount, int cursorOffset, LocalDateTime startedAt,
                              LocalDateTime finishedAt, LocalDateTime createdAt) {
        this.id = id;
        this.companyId = companyId;
        this.campaignId = campaignId;
        this.status = status;
        this.templateSnapshot = templateSnapshot;
        this.totalRecipients = totalRecipients;
        this.processedCount = processedCount;
        this.failedCount = failedCount;
        this.cursorOffset = cursorOffset;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.createdAt = createdAt;
    }

    public static CampaignExecution start(UUID companyId, UUID campaignId,
                                          String templateSnapshot, int totalRecipients) {
        LocalDateTime now = LocalDateTime.now();
        return new CampaignExecution(UUID.randomUUID(), companyId, campaignId,
                ExecutionStatus.RUNNING, templateSnapshot, totalRecipients, 0, 0, 0, now, null, now);
    }

    public static CampaignExecution reconstitute(UUID id, UUID companyId, UUID campaignId,
                                                 ExecutionStatus status, String templateSnapshot,
                                                 int totalRecipients, int processedCount,
                                                 int failedCount, int cursorOffset,
                                                 LocalDateTime startedAt, LocalDateTime finishedAt,
                                                 LocalDateTime createdAt) {
        return new CampaignExecution(id, companyId, campaignId, status, templateSnapshot,
                totalRecipients, processedCount, failedCount, cursorOffset, startedAt,
                finishedAt, createdAt);
    }

    public boolean isDispatchable() {
        return status == ExecutionStatus.RUNNING;
    }

    public void pause() {
        this.status = ExecutionStatus.PAUSED;
    }

    public void resume() {
        this.status = ExecutionStatus.RUNNING;
    }

    public void cancel() {
        this.status = ExecutionStatus.CANCELLED;
        this.finishedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = ExecutionStatus.COMPLETED;
        this.finishedAt = LocalDateTime.now();
    }

    public void recordProcessed(boolean success) {
        if (success) {
            this.processedCount++;
        } else {
            this.failedCount++;
            this.processedCount++;
        }
    }

    /** Sincroniza contadores com o estado real do banco (fonte de verdade). */
    public void syncCounters(long processedCount, long failedCount) {
        this.processedCount = (int) Math.max(0, processedCount);
        this.failedCount = (int) Math.max(0, failedCount);
    }

    public void advanceCursor(int amount) {
        this.cursorOffset += Math.max(0, amount);
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getCampaignId() { return campaignId; }
    public ExecutionStatus getStatus() { return status; }
    public String getTemplateSnapshot() { return templateSnapshot; }
    public int getTotalRecipients() { return totalRecipients; }
    public int getProcessedCount() { return processedCount; }
    public int getFailedCount() { return failedCount; }
    public int getCursorOffset() { return cursorOffset; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
