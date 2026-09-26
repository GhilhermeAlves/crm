package com.becommerce.crm.domain.ai;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regras de {@link AgentTool}, {@link ToolPermission} e
 * {@link AgentActionAudit} (WhatsApp AI Agent — Fase 1, Task 1): safe
 * defaults de criação e imutabilidade de campos-chave.
 */
class AgentToolTest {

    private final UUID companyId = UUID.randomUUID();

    // --- AgentTool -----------------------------------------------------

    @Test
    void create_shouldGenerateIdAndPreserveFields() {
        AgentTool tool = AgentTool.create(companyId, "fetchContact",
                "Fetch contact information", "read");

        assertNotNull(tool.getId());
        assertEquals(companyId, tool.getCompanyId());
        assertEquals("fetchContact", tool.getToolName());
        assertEquals("Fetch contact information", tool.getDescription());
        assertEquals("read", tool.getToolType());
        assertNotNull(tool.getCreatedAt());
        assertNotNull(tool.getUpdatedAt());
    }

    @Test
    void create_safeDefault_shouldBeDisabled() {
        AgentTool tool = AgentTool.create(companyId, "fetchContact",
                "Fetch contact information", "read");

        assertFalse(tool.isEnabled(), "uma ferramenta recém-criada deve nascer desabilitada (opt-in)");
    }

    @Test
    void enable_shouldFlipStateAndRefreshUpdatedAt() {
        AgentTool tool = AgentTool.create(companyId, "fetchContact", "desc", "read");
        LocalDateTime original = tool.getUpdatedAt();

        tool.enable();

        assertTrue(tool.isEnabled());
        assertFalse(tool.getUpdatedAt().isBefore(original));
    }

    @Test
    void disable_afterEnable_shouldFlipBack() {
        AgentTool tool = AgentTool.create(companyId, "fetchContact", "desc", "read");
        tool.enable();
        assertTrue(tool.isEnabled());

        tool.disable();

        assertFalse(tool.isEnabled());
    }

    @Test
    void reconstitute_shouldPreserveEveryField() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        AgentTool tool = AgentTool.reconstitute(id, companyId, "searchDeals",
                "Search deals", true, "read", now, now);

        assertEquals(id, tool.getId());
        assertEquals(companyId, tool.getCompanyId());
        assertEquals("searchDeals", tool.getToolName());
        assertEquals("Search deals", tool.getDescription());
        assertTrue(tool.isEnabled());
        assertEquals("read", tool.getToolType());
        assertEquals(now, tool.getCreatedAt());
        assertEquals(now, tool.getUpdatedAt());
    }

    @Test
    void id_and_companyId_shouldBeImmutable() {
        // id/companyId são `final` — não há setters expostos; o teste documenta o contrato:
        // duas instâncias criadas via create() nunca compartilham id.
        AgentTool a = AgentTool.create(companyId, "fetchContact", "desc", "read");
        AgentTool b = AgentTool.create(companyId, "fetchContact", "desc", "read");

        assertNotNull(a.getId());
        assertNotNull(b.getId());
        assertFalse(a.getId().equals(b.getId()));
    }

    // --- ToolPermission --------------------------------------------------

    @Test
    void toolPermission_create_shouldGenerateIdAndPreserveFields() {
        UUID toolId = UUID.randomUUID();

        ToolPermission perm = ToolPermission.create(toolId, "manager", true);

        assertNotNull(perm.getId());
        assertEquals(toolId, perm.getToolId());
        assertEquals("manager", perm.getRole());
        assertTrue(perm.isAllowed());
        assertNotNull(perm.getCreatedAt());
        assertNotNull(perm.getUpdatedAt());
    }

    @Test
    void toolPermission_create_safeDefault_deniedIsRespected() {
        UUID toolId = UUID.randomUUID();

        ToolPermission perm = ToolPermission.create(toolId, "agent", false);

        assertFalse(perm.isAllowed());
    }

    @Test
    void toolPermission_reconstitute_shouldPreserveEveryField() {
        UUID id = UUID.randomUUID();
        UUID toolId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        ToolPermission perm = ToolPermission.reconstitute(id, toolId, "admin", true, now, now);

        assertEquals(id, perm.getId());
        assertEquals(toolId, perm.getToolId());
        assertEquals("admin", perm.getRole());
        assertTrue(perm.isAllowed());
        assertEquals(now, perm.getCreatedAt());
        assertEquals(now, perm.getUpdatedAt());
    }

    // --- AgentActionAudit --------------------------------------------------

    @Test
    void agentActionAudit_create_shouldDefaultExecutedByToAiAgent() {
        UUID conversationId = UUID.randomUUID();
        String input = "{\"contactId\":\"123\"}";
        String result = "{\"name\":\"Joao\"}";

        AgentActionAudit audit = AgentActionAudit.create(companyId, conversationId,
                "fetchContact", input, result, "success");

        assertNotNull(audit.getId());
        assertEquals(companyId, audit.getCompanyId());
        assertEquals(conversationId, audit.getConversationId());
        assertEquals("fetchContact", audit.getToolName());
        assertEquals(input, audit.getInput());
        assertEquals(result, audit.getResult());
        assertEquals("ai-agent", audit.getExecutedBy());
        assertEquals("success", audit.getOutcome());
        assertNotNull(audit.getExecutedAt());
    }

    @Test
    void agentActionAudit_create_withExplicitExecutedBy_shouldOverrideDefault() {
        UUID conversationId = UUID.randomUUID();

        AgentActionAudit audit = AgentActionAudit.create(companyId, conversationId,
                "fetchContact", "{}", "{}", "ghilherme007@gmail.com", "success");

        assertEquals("ghilherme007@gmail.com", audit.getExecutedBy());
    }

    @Test
    void agentActionAudit_reconstitute_shouldPreserveEveryField() {
        UUID id = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        AgentActionAudit audit = AgentActionAudit.reconstitute(id, companyId, conversationId,
                "getActivity", "{\"activityId\":\"1\"}", "{\"status\":\"done\"}",
                "ai-agent", now, "failed");

        assertEquals(id, audit.getId());
        assertEquals(companyId, audit.getCompanyId());
        assertEquals(conversationId, audit.getConversationId());
        assertEquals("getActivity", audit.getToolName());
        assertEquals("{\"activityId\":\"1\"}", audit.getInput());
        assertEquals("{\"status\":\"done\"}", audit.getResult());
        assertEquals("ai-agent", audit.getExecutedBy());
        assertEquals(now, audit.getExecutedAt());
        assertEquals("failed", audit.getOutcome());
    }

    // --- ToolExecutionResult (immutable DTO, no setters) --------------------

    @Test
    void toolExecutionResult_success_shouldCarryContentAndNoError() {
        ToolExecutionResult result = ToolExecutionResult.success("{\"ok\":true}", 42L);

        assertTrue(result.isSuccess());
        assertEquals("{\"ok\":true}", result.getContent());
        assertEquals(null, result.getErrorMessage());
        assertEquals(42L, result.getExecutionTimeMs());
    }

    @Test
    void toolExecutionResult_failure_shouldCarryErrorAndNoContent() {
        ToolExecutionResult result = ToolExecutionResult.failure("not found", 7L);

        assertFalse(result.isSuccess());
        assertEquals(null, result.getContent());
        assertEquals("not found", result.getErrorMessage());
        assertEquals(7L, result.getExecutionTimeMs());
    }
}
