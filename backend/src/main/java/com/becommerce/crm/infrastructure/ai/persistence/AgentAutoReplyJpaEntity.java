package com.becommerce.crm.infrastructure.ai.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "agent_auto_replies")
public class AgentAutoReplyJpaEntity {

    @Id
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(name = "inbound_message_id")
    private UUID inboundMessageId;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }
    public UUID getInboundMessageId() { return inboundMessageId; }
    public void setInboundMessageId(UUID inboundMessageId) { this.inboundMessageId = inboundMessageId; }
    public LocalDateTime getRepliedAt() { return repliedAt; }
    public void setRepliedAt(LocalDateTime repliedAt) { this.repliedAt = repliedAt; }
}