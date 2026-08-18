package com.becommerce.crm.domain.ai;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mensagem do assistente de IA (AI-01). Correspondente à tabela
 * {@code ai_messages} (V050), protegida por RLS. {@code role} indica o emissor
 * (user/assistant/system) e {@code content} é o texto trocado.
 */
public class AiMessage {

    private final UUID id;
    private final UUID companyId;
    private final UUID conversationId;
    private final String role;
    private final String content;
    private final LocalDateTime createdAt;

    private AiMessage(UUID id, UUID companyId, UUID conversationId, String role, String content,
                      LocalDateTime createdAt) {
        this.id = id;
        this.companyId = companyId;
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static AiMessage create(UUID companyId, UUID conversationId, String role, String content) {
        return new AiMessage(UUID.randomUUID(), companyId, conversationId, role, content, LocalDateTime.now());
    }

    public static AiMessage reconstitute(UUID id, UUID companyId, UUID conversationId, String role, String content,
                                         LocalDateTime createdAt) {
        return new AiMessage(id, companyId, conversationId, role, content, createdAt);
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getConversationId() { return conversationId; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
