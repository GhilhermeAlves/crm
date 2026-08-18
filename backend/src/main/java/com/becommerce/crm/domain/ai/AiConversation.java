package com.becommerce.crm.domain.ai;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Conversa do assistente de IA (AI-01). Correspondente à tabela
 * {@code ai_conversations} (V050), protegida por RLS. Guarda o contexto básico
 * da conversa (tela e registro em foco) além do dono (empresa + usuário).
 */
public class AiConversation {

    private final UUID id;
    private final UUID companyId;
    private final UUID userId;
    private String screen;
    private UUID recordId;
    private String title;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private AiConversation(UUID id, UUID companyId, UUID userId, String screen, UUID recordId,
                           String title, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.userId = userId;
        this.screen = screen;
        this.recordId = recordId;
        this.title = title;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AiConversation create(UUID companyId, UUID userId, String screen, UUID recordId, String title) {
        LocalDateTime now = LocalDateTime.now();
        return new AiConversation(UUID.randomUUID(), companyId, userId, screen, recordId, title, now, now);
    }

    public static AiConversation reconstitute(UUID id, UUID companyId, UUID userId, String screen, UUID recordId,
                                              String title, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new AiConversation(id, companyId, userId, screen, recordId, title, createdAt, updatedAt);
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getUserId() { return userId; }
    public String getScreen() { return screen; }
    public UUID getRecordId() { return recordId; }
    public String getTitle() { return title; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
