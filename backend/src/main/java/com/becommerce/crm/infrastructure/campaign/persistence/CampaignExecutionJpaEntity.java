package com.becommerce.crm.infrastructure.campaign.persistence;

import com.becommerce.crm.domain.campaign.CampaignExecution;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "campaign_executions")
public class CampaignExecutionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "campaign_id")
    private UUID campaignId;

    @Column(name = "status")
    private String status;

    @Column(name = "template_snapshot")
    private String templateSnapshot;

    @Column(name = "total_recipients")
    private int totalRecipients;

    @Column(name = "processed_count")
    private int processedCount;

    @Column(name = "failed_count")
    private int failedCount;

    @Column(name = "cursor_offset")
    private int cursorOffset;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getCampaignId() { return campaignId; }
    public void setCampaignId(UUID campaignId) { this.campaignId = campaignId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTemplateSnapshot() { return templateSnapshot; }
    public void setTemplateSnapshot(String templateSnapshot) { this.templateSnapshot = templateSnapshot; }
    public int getTotalRecipients() { return totalRecipients; }
    public void setTotalRecipients(int totalRecipients) { this.totalRecipients = totalRecipients; }
    public int getProcessedCount() { return processedCount; }
    public void setProcessedCount(int processedCount) { this.processedCount = processedCount; }
    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
    public int getCursorOffset() { return cursorOffset; }
    public void setCursorOffset(int cursorOffset) { this.cursorOffset = cursorOffset; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
