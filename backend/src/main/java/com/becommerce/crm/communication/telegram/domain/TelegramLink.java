package com.becommerce.crm.communication.telegram.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "telegram_link")
public class TelegramLink {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "telegram_user_id", nullable = false)
    private long telegramUserId;

    @Column(name = "chat_id", nullable = false)
    private long chatId;

    @Column(name = "telegram_name")
    private String telegramName;

    @Column(name = "ai_conversation_id")
    private UUID aiConversationId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public long getTelegramUserId() { return telegramUserId; }
    public void setTelegramUserId(long telegramUserId) { this.telegramUserId = telegramUserId; }

    public long getChatId() { return chatId; }
    public void setChatId(long chatId) { this.chatId = chatId; }

    public String getTelegramName() { return telegramName; }
    public void setTelegramName(String telegramName) { this.telegramName = telegramName; }

    public UUID getAiConversationId() { return aiConversationId; }
    public void setAiConversationId(UUID aiConversationId) { this.aiConversationId = aiConversationId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
