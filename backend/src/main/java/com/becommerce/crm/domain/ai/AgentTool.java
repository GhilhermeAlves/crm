package com.becommerce.crm.domain.ai;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Ferramenta disponível para o agente de IA invocar durante o atendimento
 * (WhatsApp AI Agent — Fase 1, portabilidade Q7 → CRM). Escopada por
 * {@code companyId}, protegida por RLS.
 *
 * <p>Regra de segurança (safe default): uma ferramenta recém-criada nasce
 * {@code enabled = false} — nenhuma empresa ganha acesso a uma ferramenta sem
 * habilitação explícita (opt-in).</p>
 */
public class AgentTool {

    private final UUID id;
    private final UUID companyId;
    private final String toolName;
    private final String description;
    private boolean enabled;
    private final String toolType;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private AgentTool(UUID id, UUID companyId, String toolName, String description,
                      boolean enabled, String toolType, LocalDateTime createdAt,
                      LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.toolName = toolName;
        this.description = description;
        this.enabled = enabled;
        this.toolType = toolType;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Cria uma nova ferramenta. Safe default: {@code enabled = false}. */
    public static AgentTool create(UUID companyId, String toolName, String description,
                                   String toolType) {
        LocalDateTime now = LocalDateTime.now();
        return new AgentTool(UUID.randomUUID(), companyId, toolName, description,
                false, toolType, now, now);
    }

    public static AgentTool reconstitute(UUID id, UUID companyId, String toolName,
                                         String description, boolean enabled,
                                         String toolType, LocalDateTime createdAt,
                                         LocalDateTime updatedAt) {
        return new AgentTool(id, companyId, toolName, description, enabled,
                toolType, createdAt, updatedAt);
    }

    /** Habilita a ferramenta para a empresa (opt-in explícito). */
    public void enable() {
        this.enabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    /** Desabilita a ferramenta (o agente deixa de poder invocá-la). */
    public void disable() {
        this.enabled = false;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getToolName() { return toolName; }
    public String getDescription() { return description; }
    public String getToolType() { return toolType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgentTool other)) return false;
        return enabled == other.enabled
                && Objects.equals(id, other.id)
                && Objects.equals(companyId, other.companyId)
                && Objects.equals(toolName, other.toolName)
                && Objects.equals(description, other.description)
                && Objects.equals(toolType, other.toolType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, companyId, toolName, description, enabled, toolType);
    }
}
