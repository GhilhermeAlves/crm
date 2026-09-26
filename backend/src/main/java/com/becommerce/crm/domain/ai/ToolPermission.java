package com.becommerce.crm.domain.ai;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Controle de acesso baseado em papel (role) para uma {@link AgentTool}
 * (WhatsApp AI Agent — Fase 1). Permite restringir quais papéis do CRM
 * ({@code admin}, {@code manager}, {@code agent}, ...) podem ter uma
 * ferramenta habilitada para si.
 *
 * <p>Safe default: a ausência de um {@code ToolPermission} para um papel deve
 * ser tratada pela camada de aplicação como "não permitido" — esta entidade
 * apenas representa o registro explícito de permissão (allow/deny).</p>
 */
public class ToolPermission {

    private final UUID id;
    private final UUID toolId;
    private final String role;
    private final boolean allowed;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private ToolPermission(UUID id, UUID toolId, String role, boolean allowed,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.toolId = toolId;
        this.role = role;
        this.allowed = allowed;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ToolPermission create(UUID toolId, String role, boolean allowed) {
        LocalDateTime now = LocalDateTime.now();
        return new ToolPermission(UUID.randomUUID(), toolId, role, allowed, now, now);
    }

    public static ToolPermission reconstitute(UUID id, UUID toolId, String role,
                                              boolean allowed, LocalDateTime createdAt,
                                              LocalDateTime updatedAt) {
        return new ToolPermission(id, toolId, role, allowed, createdAt, updatedAt);
    }

    public UUID getId() { return id; }
    public UUID getToolId() { return toolId; }
    public String getRole() { return role; }
    public boolean isAllowed() { return allowed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ToolPermission other)) return false;
        return allowed == other.allowed
                && Objects.equals(id, other.id)
                && Objects.equals(toolId, other.toolId)
                && Objects.equals(role, other.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, toolId, role, allowed);
    }
}
