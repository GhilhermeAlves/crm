package com.becommerce.crm.infrastructure.ai.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "agent_config")
public class AgentConfigJpaEntity {

    @Id
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "ai_enabled")
    private boolean aiEnabled;

    @Column(name = "allow_auto_reply")
    private boolean allowAutoReply;

    @Column(name = "system_prompt")
    private String systemPrompt;

    @Column(name = "model")
    private String model;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "max_tokens")
    private Integer maxTokens;

    @Column(name = "cooldown_minutes")
    private int cooldownMinutes;

    @Column(name = "max_chars")
    private int maxChars;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public boolean isAiEnabled() { return aiEnabled; }
    public void setAiEnabled(boolean aiEnabled) { this.aiEnabled = aiEnabled; }
    public boolean isAllowAutoReply() { return allowAutoReply; }
    public void setAllowAutoReply(boolean allowAutoReply) { this.allowAutoReply = allowAutoReply; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public int getCooldownMinutes() { return cooldownMinutes; }
    public void setCooldownMinutes(int cooldownMinutes) { this.cooldownMinutes = cooldownMinutes; }
    public int getMaxChars() { return maxChars; }
    public void setMaxChars(int maxChars) { this.maxChars = maxChars; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}