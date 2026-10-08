package com.becommerce.crm.automation.ai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    @Column(name = "voice_reply_mode")
    private String voiceReplyMode = "MIRROR";

    @Column(name = "max_chars")
    private int maxChars;

    @Column(name = "agent_name")
    private String agentName;

    @Column(name = "agent_description")
    private String agentDescription;

    @Column(name = "persona")
    private String persona;

    @Column(name = "objective")
    private String objective;

    @Column(name = "tone")
    private String tone;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rules", columnDefinition = "jsonb")
    private List<String> rules = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "instructions", columnDefinition = "jsonb")
    private List<String> instructions = new ArrayList<>();

    @Column(name = "memory_enabled")
    private boolean memoryEnabled;

    @Column(name = "human_transfer_enabled")
    private boolean humanTransferEnabled;

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
    public String getVoiceReplyMode() { return voiceReplyMode; }
    public void setVoiceReplyMode(String voiceReplyMode) { this.voiceReplyMode = voiceReplyMode; }
    public int getMaxChars() { return maxChars; }
    public void setMaxChars(int maxChars) { this.maxChars = maxChars; }
    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }
    public String getAgentDescription() { return agentDescription; }
    public void setAgentDescription(String agentDescription) { this.agentDescription = agentDescription; }
    public String getPersona() { return persona; }
    public void setPersona(String persona) { this.persona = persona; }
    public String getObjective() { return objective; }
    public void setObjective(String objective) { this.objective = objective; }
    public String getTone() { return tone; }
    public void setTone(String tone) { this.tone = tone; }
    public List<String> getRules() { return rules; }
    public void setRules(List<String> rules) { this.rules = rules; }
    public List<String> getInstructions() { return instructions; }
    public void setInstructions(List<String> instructions) { this.instructions = instructions; }
    public boolean isMemoryEnabled() { return memoryEnabled; }
    public void setMemoryEnabled(boolean memoryEnabled) { this.memoryEnabled = memoryEnabled; }
    public boolean isHumanTransferEnabled() { return humanTransferEnabled; }
    public void setHumanTransferEnabled(boolean humanTransferEnabled) { this.humanTransferEnabled = humanTransferEnabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}