package com.becommerce.crm.automation.ai.domain;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Memória do agente sobre um contato (tabela {@code agent_memory}, V088, RLS).
 * Sempre escopada por empresa + agente + contato: nunca é recuperada fora desse
 * contexto. Não é histórico de conversa nem substitui dados transacionais do CRM.
 */
public class AgentMemory {

    public static final int MAX_CONTENT_LENGTH = 500;
    public static final int MIN_IMPORTANCE = 1;
    public static final int MAX_IMPORTANCE = 5;
    public static final int DEFAULT_IMPORTANCE = 3;

    private final UUID id;
    private final UUID companyId;
    private final UUID agentConfigId;
    private final UUID contactId;
    private MemoryType type;
    private String content;
    private int importance;
    private final MemorySource source;
    private final UUID sourceMessageId;
    private final Map<String, Object> metadata;
    private LocalDateTime expiresAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private AgentMemory(UUID id, UUID companyId, UUID agentConfigId, UUID contactId, MemoryType type,
                        String content, int importance, MemorySource source, UUID sourceMessageId,
                        Map<String, Object> metadata, LocalDateTime expiresAt,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = require(companyId, "companyId");
        this.agentConfigId = require(agentConfigId, "agentConfigId");
        this.contactId = require(contactId, "contactId");
        this.type = require(type, "type");
        this.content = validContent(content);
        this.importance = validImportance(importance);
        this.source = source == null ? MemorySource.AGENT : source;
        this.sourceMessageId = sourceMessageId;
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AgentMemory create(UUID companyId, UUID agentConfigId, UUID contactId, MemoryType type,
                                     String content, Integer importance, MemorySource source,
                                     UUID sourceMessageId, Map<String, Object> metadata,
                                     LocalDateTime expiresAt) {
        LocalDateTime now = LocalDateTime.now();
        return new AgentMemory(UUID.randomUUID(), companyId, agentConfigId, contactId, type, content,
                importance == null ? DEFAULT_IMPORTANCE : importance, source, sourceMessageId, metadata,
                expiresAt, now, now);
    }

    public static AgentMemory reconstitute(UUID id, UUID companyId, UUID agentConfigId, UUID contactId,
                                           MemoryType type, String content, int importance,
                                           MemorySource source, UUID sourceMessageId,
                                           Map<String, Object> metadata, LocalDateTime expiresAt,
                                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new AgentMemory(id, companyId, agentConfigId, contactId, type, content, importance, source,
                sourceMessageId, metadata, expiresAt, createdAt, updatedAt);
    }

    /** Atualiza tipo/conteúdo/importância/expiração (nulos mantêm o valor atual). */
    public void update(MemoryType type, String content, Integer importance, LocalDateTime expiresAt) {
        if (type != null) {
            this.type = type;
        }
        if (content != null) {
            this.content = validContent(content);
        }
        if (importance != null) {
            this.importance = validImportance(importance);
        }
        if (expiresAt != null) {
            this.expiresAt = expiresAt;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt != null && !now.isBefore(expiresAt);
    }

    /** Defesa em profundidade além do RLS: a memória pertence a esta empresa e contato? */
    public boolean belongsTo(UUID companyId, UUID contactId) {
        return this.companyId.equals(companyId) && this.contactId.equals(contactId);
    }

    private static String validContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Conteúdo da memória é obrigatório");
        }
        String trimmed = content.trim();
        if (trimmed.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Memória excede " + MAX_CONTENT_LENGTH + " caracteres");
        }
        return trimmed;
    }

    private static int validImportance(int importance) {
        if (importance < MIN_IMPORTANCE || importance > MAX_IMPORTANCE) {
            throw new IllegalArgumentException("Importância deve estar entre 1 e 5");
        }
        return importance;
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " é obrigatório");
        }
        return value;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getAgentConfigId() { return agentConfigId; }
    public UUID getContactId() { return contactId; }
    public MemoryType getType() { return type; }
    public String getContent() { return content; }
    public int getImportance() { return importance; }
    public MemorySource getSource() { return source; }
    public UUID getSourceMessageId() { return sourceMessageId; }
    public Map<String, Object> getMetadata() { return metadata; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
