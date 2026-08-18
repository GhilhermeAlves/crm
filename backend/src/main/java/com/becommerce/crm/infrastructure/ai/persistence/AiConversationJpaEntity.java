package com.becommerce.crm.infrastructure.ai.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_conversations")
public class AiConversationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "screen")
    private String screen;

    @Column(name = "record_id")
    private UUID recordId;

    @Column(name = "title")
    private String title;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getScreen() { return screen; }
    public void setScreen(String screen) { this.screen = screen; }
    public UUID getRecordId() { return recordId; }
    public void setRecordId(UUID recordId) { this.recordId = recordId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
