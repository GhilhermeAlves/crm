package com.becommerce.crm.communication.telegram.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "telegram_bot_config")
public class TelegramBotConfig {

    @Id @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "bot_token_cipher", nullable = false)
    private String botTokenCipher;

    @Column(name = "webhook_secret_hash", nullable = false)
    private String webhookSecretHash;

    @Column(name = "bot_username", nullable = false)
    private String botUsername;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public String getBotTokenCipher() { return botTokenCipher; }
    public void setBotTokenCipher(String botTokenCipher) { this.botTokenCipher = botTokenCipher; }

    public String getWebhookSecretHash() { return webhookSecretHash; }
    public void setWebhookSecretHash(String webhookSecretHash) { this.webhookSecretHash = webhookSecretHash; }

    public String getBotUsername() { return botUsername; }
    public void setBotUsername(String botUsername) { this.botUsername = botUsername; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
