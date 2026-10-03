package com.becommerce.crm.automation.ai.infrastructure.persistence;

import com.becommerce.crm.automation.ai.domain.AgentActionAudit;
import com.becommerce.crm.automation.ai.domain.AgentTool;
import com.becommerce.crm.automation.ai.domain.ToolPermission;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Round-trip domínio ↔ JPA das tabelas do agente (V077): nenhum campo se perde. */
class AgentToolPersistenceMappingTest {

    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 9, 26, 10, 0);
    private static final LocalDateTime UPDATED = LocalDateTime.of(2026, 9, 26, 11, 30);

    @Test
    void agentToolRoundTrip() {
        AgentTool tool = AgentTool.reconstitute(UUID.randomUUID(), UUID.randomUUID(), "fetchContact",
                "Busca contato", true, "READ_ONLY", CREATED, UPDATED);

        AgentTool back = AgentToolRepositoryImpl.toDomain(AgentToolRepositoryImpl.toEntity(tool));

        assertEquals(tool.getId(), back.getId());
        assertEquals(tool.getCompanyId(), back.getCompanyId());
        assertEquals("fetchContact", back.getToolName());
        assertEquals("Busca contato", back.getDescription());
        assertEquals(true, back.isEnabled());
        assertEquals("READ_ONLY", back.getToolType());
        assertEquals(CREATED, back.getCreatedAt());
        assertEquals(UPDATED, back.getUpdatedAt());
    }

    @Test
    void toolPermissionRoundTrip() {
        ToolPermission permission = ToolPermission.reconstitute(UUID.randomUUID(), UUID.randomUUID(),
                "MANAGER", true, CREATED, UPDATED);

        ToolPermission back = ToolPermissionRepositoryImpl.toDomain(
                ToolPermissionRepositoryImpl.toEntity(permission));

        assertEquals(permission.getId(), back.getId());
        assertEquals(permission.getToolId(), back.getToolId());
        assertEquals("MANAGER", back.getRole());
        assertEquals(true, back.isAllowed());
        assertEquals(CREATED, back.getCreatedAt());
        assertEquals(UPDATED, back.getUpdatedAt());
    }

    @Test
    void agentActionAuditRoundTrip() {
        AgentActionAudit audit = AgentActionAudit.reconstitute(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "searchDeals", "{\"q\":\"x\"}", "{\"n\":1}", "ai-agent", CREATED,
                "SUCCESS");

        AgentActionAudit back = AgentActionAuditRepositoryImpl.toDomain(
                AgentActionAuditRepositoryImpl.toEntity(audit));

        assertEquals(audit.getId(), back.getId());
        assertEquals(audit.getCompanyId(), back.getCompanyId());
        assertEquals(audit.getConversationId(), back.getConversationId());
        assertEquals("searchDeals", back.getToolName());
        assertEquals("{\"q\":\"x\"}", back.getInput());
        assertEquals("{\"n\":1}", back.getResult());
        assertEquals("ai-agent", back.getExecutedBy());
        assertEquals(CREATED, back.getExecutedAt());
        assertEquals("SUCCESS", back.getOutcome());
    }
}
