package com.becommerce.crm.automation.ai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "agent_memory")
public class AgentMemoryJpaEntity {

    @Id
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "agent_config_id")
    private UUID agentConfigId;

    @Column(name = "contact_id")
    private UUID contactId;

    @Column(name = "memory_type")
    private String memoryType;

    @Column(name = "content")
    private String content;

    @Column(name = "importance")
    private short importance;

    @Column(name = "source")
    private String source;

    @Column(name = "source_message_id")
    private UUID sourceMessageId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getAgentConfigId() { return agentConfigId; }
    public void setAgentConfigId(UUID agentConfigId) { this.agentConfigId = agentConfigId; }
    public UUID getContactId() { return contactId; }
    public void setContactId(UUID contactId) { this.contactId = contactId; }
    public String getMemoryType() { return memoryType; }
    public void setMemoryType(String memoryType) { this.memoryType = memoryType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public short getImportance() { return importance; }
    public void setImportance(short importance) { this.importance = importance; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public UUID getSourceMessageId() { return sourceMessageId; }
    public void setSourceMessageId(UUID sourceMessageId) { this.sourceMessageId = sourceMessageId; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
