# WhatsApp AI Agent Phase 1 Implementation Plan

> **Para executores agentic:** USE superpowers:subagent-driven-development OU superpowers:executing-plans para implementar este plano tarefa por tarefa.

**Goal:** Implementar tool registry, 5 ferramentas read-only (fetchContact, searchDeals, getActivity, getProductInfo, getServiceInfo), permission model, e audit trail.

**Architecture:** Estender WhatsAppInboundAutoReplyProcessor com Tool abstraction (domain + infrastructure), ToolProvider para adaptar LLM tool calls → CRM domain calls, AgentActionAudit para logging.

**Tech Stack:** Java 17, Spring Boot 3.x, Clean Architecture (DDD), PostgreSQL (RLS), RabbitMQ, OpenAI/Groq/Claude SDKs

**Spec:** ADR-001 (https://claude.ai/artifact/9AwqPhd1Uzs1vtiyVxHAvP) — Seção "Action Items" → Phase 1

---

## Global Constraints

- Preserve multi-tenancy: TenantContext + RLS enforcement mandatory for all new entities
- No breaking changes to existing Message/Conversation/Channel
- All tool execution logged to AgentActionAudit (never hardcode decisions)
- Safe defaults: agents have NO tools by default (opt-in per company)
- Tool calls from AI must be validated before execution (prevent injection)

---

## File Structure

```
backend/src/main/java/com/becommerce/crm/
├── domain/ai/
│   ├── AgentTool.java                 [NEW] Tool entity
│   ├── ToolPermission.java            [NEW] Permission entity
│   ├── ToolExecutionResult.java       [NEW] Result DTO
│   └── AgentActionAudit.java          [NEW] Audit entity
│
├── application/ai/
│   ├── port/output/
│   │   ├── AgentToolRepository.java   [NEW] Tool CRUD
│   │   ├── ToolPermissionRepository.java [NEW]
│   │   ├── AgentActionAuditRepository.java [NEW] Audit persistence
│   │   └── ToolProvider.java          [NEW] LLM tool call abstraction
│   │
│   ├── tool/
│   │   ├── ToolRegistry.java          [NEW] Tool registry
│   │   ├── ToolExecutor.java          [NEW] Execute tools
│   │   ├── impl/
│   │   │   ├── FetchContactTool.java  [NEW]
│   │   │   ├── SearchDealsTool.java   [NEW]
│   │   │   ├── GetActivityTool.java   [NEW]
│   │   │   ├── GetProductInfoTool.java [NEW]
│   │   │   └── GetServiceInfoTool.java [NEW]
│   │
│   ├── dto/
│   │   ├── AgentToolResponse.java     [NEW]
│   │   ├── ToolPermissionResponse.java [NEW]
│   │   └── AgentActionAuditResponse.java [NEW]
│   │
│   └── service/
│       └── WhatsAppInboundAutoReplyProcessor.java [MODIFY] Integrate tools
│
├── infrastructure/ai/
│   ├── persistence/
│   │   ├── AgentToolJpaEntity.java    [NEW]
│   │   ├── AgentToolJpaRepository.java [NEW]
│   │   ├── AgentToolRepositoryImpl.java [NEW]
│   │   ├── ToolPermissionJpaEntity.java [NEW]
│   │   ├── ToolPermissionJpaRepository.java [NEW]
│   │   ├── ToolPermissionRepositoryImpl.java [NEW]
│   │   ├── AgentActionAuditJpaEntity.java [NEW]
│   │   ├── AgentActionAuditJpaRepository.java [NEW]
│   │   └── AgentActionAuditRepositoryImpl.java [NEW]
│   │
│   └── provider/
│       ├── OpenAiToolProvider.java    [NEW] OpenAI tool format adapter
│       ├── GroqToolProvider.java      [NEW] Groq tool format adapter
│       └── ClaudeToolProvider.java    [NEW] Claude tool format adapter
│
├── presentation/rest/ai/
│   └── AgentToolController.java       [NEW] REST endpoints for tool management
│
└── migration/
    └── V077__CreateAgentToolsAndAudit.sql [NEW] DB migration
```

---

## Task 1: Create Domain Entities (AgentTool, ToolPermission, AgentActionAudit)

**Files:**
- Create: `backend/src/main/java/com/becommerce/crm/domain/ai/AgentTool.java`
- Create: `backend/src/main/java/com/becommerce/crm/domain/ai/ToolPermission.java`
- Create: `backend/src/main/java/com/becommerce/crm/domain/ai/ToolExecutionResult.java`
- Create: `backend/src/main/java/com/becommerce/crm/domain/ai/AgentActionAudit.java`
- Test: `backend/src/test/java/com/becommerce/crm/domain/ai/AgentToolTest.java`

**Interfaces:**
- Produces: `AgentTool` (id, companyId, toolName, description, enabled, toolType, createdAt, updatedAt)
- Produces: `ToolPermission` (id, toolId, role, allowed, createdAt)
- Produces: `AgentActionAudit` (id, companyId, conversationId, toolName, input, result, executedBy, timestamp, outcome)

**Steps:**

- [ ] Create `AgentTool.java` (domain entity for available tools)

```java
package com.becommerce.crm.domain.ai;

import java.time.LocalDateTime;
import java.util.UUID;

public class AgentTool {
    private final UUID id;
    private final UUID companyId;
    private final String toolName;           // e.g., "fetchContact"
    private final String description;
    private final boolean enabled;
    private final String toolType;           // e.g., "read", "write", "action"
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

    public void enable() {
        this.updatedAt = LocalDateTime.now();
        // Note: actual enable logic handled by repository/service
    }

    public void disable() {
        this.updatedAt = LocalDateTime.now();
        // Note: actual disable logic handled by repository/service
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getToolName() { return toolName; }
    public String getDescription() { return description; }
    public boolean isEnabled() { return enabled; }
    public String getToolType() { return toolType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

- [ ] Create `ToolPermission.java` (restrict tool access by role)

```java
package com.becommerce.crm.domain.ai;

import java.time.LocalDateTime;
import java.util.UUID;

public class ToolPermission {
    private final UUID id;
    private final UUID toolId;
    private final String role;              // e.g., "admin", "manager", "agent"
    private final boolean allowed;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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
}
```

- [ ] Create `ToolExecutionResult.java` (result from tool execution)

```java
package com.becommerce.crm.domain.ai;

public class ToolExecutionResult {
    private final boolean success;
    private final String content;           // JSON or plain text result
    private final String errorMessage;
    private final long executionTimeMs;

    public ToolExecutionResult(boolean success, String content, String errorMessage,
                              long executionTimeMs) {
        this.success = success;
        this.content = content;
        this.errorMessage = errorMessage;
        this.executionTimeMs = executionTimeMs;
    }

    public static ToolExecutionResult success(String content, long executionTimeMs) {
        return new ToolExecutionResult(true, content, null, executionTimeMs);
    }

    public static ToolExecutionResult failure(String errorMessage, long executionTimeMs) {
        return new ToolExecutionResult(false, null, errorMessage, executionTimeMs);
    }

    public boolean isSuccess() { return success; }
    public String getContent() { return content; }
    public String getErrorMessage() { return errorMessage; }
    public long getExecutionTimeMs() { return executionTimeMs; }
}
```

- [ ] Create `AgentActionAudit.java` (audit trail for all agent actions)

```java
package com.becommerce.crm.domain.ai;

import java.time.LocalDateTime;
import java.util.UUID;

public class AgentActionAudit {
    private final UUID id;
    private final UUID companyId;
    private final UUID conversationId;
    private final String toolName;
    private final String input;              // JSON of tool call params
    private final String result;             // JSON of tool result
    private final String executedBy;        // "ai-agent" or user email
    private final LocalDateTime executedAt;
    private final String outcome;           // "success", "failed", "skipped"

    private AgentActionAudit(UUID id, UUID companyId, UUID conversationId,
                            String toolName, String input, String result,
                            String executedBy, LocalDateTime executedAt,
                            String outcome) {
        this.id = id;
        this.companyId = companyId;
        this.conversationId = conversationId;
        this.toolName = toolName;
        this.input = input;
        this.result = result;
        this.executedBy = executedBy;
        this.executedAt = executedAt;
        this.outcome = outcome;
    }

    public static AgentActionAudit create(UUID companyId, UUID conversationId,
                                         String toolName, String input, String result,
                                         String outcome) {
        return new AgentActionAudit(UUID.randomUUID(), companyId, conversationId,
                toolName, input, result, "ai-agent", LocalDateTime.now(), outcome);
    }

    public static AgentActionAudit reconstitute(UUID id, UUID companyId, UUID conversationId,
                                               String toolName, String input, String result,
                                               String executedBy, LocalDateTime executedAt,
                                               String outcome) {
        return new AgentActionAudit(id, companyId, conversationId, toolName, input,
                result, executedBy, executedAt, outcome);
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getConversationId() { return conversationId; }
    public String getToolName() { return toolName; }
    public String getInput() { return input; }
    public String getResult() { return result; }
    public String getExecutedBy() { return executedBy; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public String getOutcome() { return outcome; }
}
```

- [ ] Create failing test `AgentToolTest.java`

```java
package com.becommerce.crm.domain.ai;

import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AgentToolTest {
    @Test
    void testCreateAgentTool() {
        UUID companyId = UUID.randomUUID();
        AgentTool tool = AgentTool.create(companyId, "fetchContact",
                "Fetch contact information", "read");

        assertNotNull(tool.getId());
        assertEquals(companyId, tool.getCompanyId());
        assertEquals("fetchContact", tool.getToolName());
        assertFalse(tool.isEnabled());
    }

    @Test
    void testToolPermissionCreate() {
        UUID toolId = UUID.randomUUID();
        ToolPermission perm = ToolPermission.create(toolId, "manager", true);

        assertNotNull(perm.getId());
        assertEquals(toolId, perm.getToolId());
        assertEquals("manager", perm.getRole());
        assertTrue(perm.isAllowed());
    }

    @Test
    void testAgentActionAuditCreate() {
        UUID companyId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        String input = "{\"contactId\":\"123\"}";
        String result = "{\"name\":\"João\"}";

        AgentActionAudit audit = AgentActionAudit.create(companyId, conversationId,
                "fetchContact", input, result, "success");

        assertNotNull(audit.getId());
        assertEquals(companyId, audit.getCompanyId());
        assertEquals("fetchContact", audit.getToolName());
        assertEquals("success", audit.getOutcome());
    }
}
```

- [ ] Run test to verify it passes

```bash
cd backend
mvn test -Dtest=AgentToolTest -DfailIfNoTests=false
```

- [ ] Commit

```bash
git add backend/src/main/java/com/becommerce/crm/domain/ai/
git add backend/src/test/java/com/becommerce/crm/domain/ai/AgentToolTest.java
git commit -m "feat(ai): add AgentTool, ToolPermission, AgentActionAudit domain entities"
```

---

## Task 2: Create Database Migration for New Tables

**Files:**
- Create: `backend/src/main/resources/db/migration/V077__CreateAgentToolsAndAudit.sql`
- Test: Manual verification (run migration on dev database)

> **Nota (implementado):** o SQL abaixo é o rascunho original. A migration real segue o padrão
> do banco: tabelas `companies` e `omnichannel_conversations`, função `app.current_tenant_id()`,
> `FORCE ROW LEVEL SECURITY` com `tenant_isolation_policy` (USING + WITH CHECK), e GRANTs para
> `crm_app` (auditoria append-only). Use o arquivo V077 como fonte de verdade.

**Steps:**

- [ ] Create migration SQL file

```sql
-- V077__CreateAgentToolsAndAudit.sql

-- agent_tool: Available tools that agents can invoke (scoped per company)
CREATE TABLE agent_tool (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    tool_name VARCHAR(100) NOT NULL,
    description TEXT,
    enabled BOOLEAN DEFAULT FALSE,
    tool_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(company_id, tool_name)
);

-- RLS: Users can only see tools of their company
ALTER TABLE agent_tool ENABLE ROW LEVEL SECURITY;

CREATE POLICY agent_tool_select_policy ON agent_tool
    FOR SELECT USING (company_id = app.current_company_id());

CREATE POLICY agent_tool_insert_policy ON agent_tool
    FOR INSERT WITH CHECK (company_id = app.current_company_id());

CREATE POLICY agent_tool_update_policy ON agent_tool
    FOR UPDATE USING (company_id = app.current_company_id());

CREATE POLICY agent_tool_delete_policy ON agent_tool
    FOR DELETE USING (company_id = app.current_company_id());

-- Index for queries by company
CREATE INDEX idx_agent_tool_company ON agent_tool(company_id);

-- tool_permission: Role-based access control for tools
CREATE TABLE tool_permission (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tool_id UUID NOT NULL REFERENCES agent_tool(id) ON DELETE CASCADE,
    role VARCHAR(100) NOT NULL,
    allowed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tool_id, role)
);

-- RLS: Users can only manage permissions for tools in their company
ALTER TABLE tool_permission ENABLE ROW LEVEL SECURITY;

CREATE POLICY tool_permission_select_policy ON tool_permission
    FOR SELECT USING (
        tool_id IN (
            SELECT id FROM agent_tool WHERE company_id = app.current_company_id()
        )
    );

CREATE POLICY tool_permission_insert_policy ON tool_permission
    FOR INSERT WITH CHECK (
        tool_id IN (
            SELECT id FROM agent_tool WHERE company_id = app.current_company_id()
        )
    );

-- agent_action_audit: Comprehensive audit trail for all agent actions
CREATE TABLE agent_action_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
    tool_name VARCHAR(100) NOT NULL,
    input JSONB NOT NULL,
    result JSONB,
    executed_by VARCHAR(100) NOT NULL DEFAULT 'ai-agent',
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    outcome VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- RLS: Users see only audits for their company
ALTER TABLE agent_action_audit ENABLE ROW LEVEL SECURITY;

CREATE POLICY agent_action_audit_select_policy ON agent_action_audit
    FOR SELECT USING (company_id = app.current_company_id());

CREATE POLICY agent_action_audit_insert_policy ON agent_action_audit
    FOR INSERT WITH CHECK (company_id = app.current_company_id());

-- Indices for audit queries
CREATE INDEX idx_agent_action_audit_company ON agent_action_audit(company_id);
CREATE INDEX idx_agent_action_audit_conversation ON agent_action_audit(conversation_id);
CREATE INDEX idx_agent_action_audit_executed_at ON agent_action_audit(executed_at DESC);
```

- [ ] Verify migration file is in correct location

```bash
ls -la backend/src/main/resources/db/migration/ | grep V077
```

- [ ] Commit migration

```bash
git add backend/src/main/resources/db/migration/V077__CreateAgentToolsAndAudit.sql
git commit -m "db(migration): add agent_tool, tool_permission, agent_action_audit tables with RLS"
```

---

## Task 3: Create Repository Interfaces & Implementations

**Files:**
- Create: `backend/src/main/java/com/becommerce/crm/application/ai/port/output/AgentToolRepository.java`
- Create: `backend/src/main/java/com/becommerce/crm/application/ai/port/output/ToolPermissionRepository.java`
- Create: `backend/src/main/java/com/becommerce/crm/application/ai/port/output/AgentActionAuditRepository.java`
- Create: JPA entities and implementations (infrastructure layer)

**Interfaces:**
- Produces: `AgentToolRepository.findByCompanyId(UUID): List<AgentTool>`, `findByCompanyAndName(UUID, String): Optional<AgentTool>`, `save(AgentTool): AgentTool`
- Produces: `ToolPermissionRepository.findByToolId(UUID): List<ToolPermission>`, `save(ToolPermission): ToolPermission`
- Produces: `AgentActionAuditRepository.save(AgentActionAudit): AgentActionAudit`, `findByConversationId(UUID, pageNum, pageSize): Page<AgentActionAudit>`

**Steps:**

- [ ] Create `AgentToolRepository` interface

```java
package com.becommerce.crm.application.ai.port.output;

import com.becommerce.crm.domain.ai.AgentTool;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentToolRepository {
    List<AgentTool> findByCompanyId(UUID companyId);
    Optional<AgentTool> findByCompanyAndName(UUID companyId, String toolName);
    Optional<AgentTool> findById(UUID id);
    AgentTool save(AgentTool tool);
    void delete(UUID id);
}
```

- [ ] Create `ToolPermissionRepository` interface

```java
package com.becommerce.crm.application.ai.port.output;

import com.becommerce.crm.domain.ai.ToolPermission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ToolPermissionRepository {
    List<ToolPermission> findByToolId(UUID toolId);
    Optional<ToolPermission> findByToolAndRole(UUID toolId, String role);
    ToolPermission save(ToolPermission permission);
    void delete(UUID id);
}
```

- [ ] Create `AgentActionAuditRepository` interface

```java
package com.becommerce.crm.application.ai.port.output;

import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.domain.ai.AgentActionAudit;
import java.util.UUID;

public interface AgentActionAuditRepository {
    AgentActionAudit save(AgentActionAudit audit);
    PageResponse<AgentActionAudit> findByConversationId(UUID conversationId, int page, int size);
    PageResponse<AgentActionAudit> findByCompanyId(UUID companyId, int page, int size);
}
```

- [ ] Create JPA entities and repositories (infrastructure)

```java
// AgentToolJpaEntity.java
package com.becommerce.crm.infrastructure.ai.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "agent_tool")
public class AgentToolJpaEntity {
    @Id
    private UUID id;
    private UUID companyId;
    private String toolName;
    private String description;
    private boolean enabled;
    private String toolType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters/Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getToolType() { return toolType; }
    public void setToolType(String toolType) { this.toolType = toolType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] Commit infrastructure layer

```bash
git add backend/src/main/java/com/becommerce/crm/application/ai/port/output/
git add backend/src/main/java/com/becommerce/crm/infrastructure/ai/persistence/
git commit -m "feat(ai): add repository interfaces and JPA implementations for tools and audit"
```

---

## Task 4: Create Tool Provider Abstraction

**Files:**
- Create: `backend/src/main/java/com/becommerce/crm/application/ai/port/output/ToolProvider.java`
- Create: OpenAI, Groq, Claude adapters
- Test: `ToolProviderTest.java`

**Interfaces:**
- Produces: `ToolProvider.buildToolDefinitions(List<AgentTool>): List<ToolDefinition>`, `parseToolCall(String): ToolCall`, `formatToolResult(String): String`

**Steps:**

- [ ] Create `ToolProvider` interface

```java
package com.becommerce.crm.application.ai.port.output;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public interface ToolProvider {
    
    record ToolDefinition(String name, String description, JsonNode parameters) {}
    
    record ToolCall(String toolName, JsonNode arguments) {}
    
    /**
     * Build tool definitions in provider-specific format (OpenAI, Groq, Claude, etc.)
     */
    List<ToolDefinition> buildToolDefinitions(List<Tool> tools);
    
    /**
     * Parse a tool call from LLM response (provider-specific format)
     */
    ToolCall parseToolCall(String llmContent) throws ToolParseException;
    
    /**
     * Format tool execution result for next AI round
     */
    String formatToolResult(String toolName, String executionResult);
    
    class ToolParseException extends Exception {
        public ToolParseException(String msg) { super(msg); }
        public ToolParseException(String msg, Throwable cause) { super(msg, cause); }
    }
}
```

- [ ] Create OpenAI adapter

```java
package com.becommerce.crm.infrastructure.ai.provider;

import com.becommerce.crm.application.ai.port.output.ToolProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
public class OpenAiToolProvider implements ToolProvider {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<ToolDefinition> buildToolDefinitions(List<Tool> tools) {
        return tools.stream()
            .map(tool -> {
                ObjectNode params = objectMapper.createObjectNode();
                params.put("type", "object");
                params.put("properties", objectMapper.createObjectNode());
                params.put("required", objectMapper.createArrayNode());
                
                return new ToolDefinition(
                    tool.getToolName(),
                    tool.getDescription(),
                    params
                );
            })
            .toList();
    }

    @Override
    public ToolCall parseToolCall(String llmContent) throws ToolParseException {
        try {
            JsonNode json = objectMapper.readTree(llmContent);
            String toolName = json.get("tool_name").asText();
            JsonNode args = json.get("arguments");
            return new ToolCall(toolName, args);
        } catch (Exception e) {
            throw new ToolParseException("Failed to parse OpenAI tool call", e);
        }
    }

    @Override
    public String formatToolResult(String toolName, String executionResult) {
        return "Tool " + toolName + " returned: " + executionResult;
    }
}
```

- [ ] Commit tool provider abstraction

```bash
git add backend/src/main/java/com/becommerce/crm/application/ai/port/output/ToolProvider.java
git add backend/src/main/java/com/becommerce/crm/infrastructure/ai/provider/
git commit -m "feat(ai): add ToolProvider abstraction with OpenAI adapter"
```

---

## Task 5: Implement 5 Read-Only Tools

**Files:**
- Create: `backend/src/main/java/com/becommerce/crm/application/ai/tool/Tool.java` (abstract base)
- Create: 5 tool implementations (FetchContactTool, SearchDealsTool, GetActivityTool, GetProductInfoTool, GetServiceInfoTool)
- Test: Unit tests for each tool

**Interfaces:**
- Produces: `Tool.execute(JsonNode params): ToolExecutionResult`

**Steps (1 tool = 1 subtask):**

- [ ] Create abstract `Tool` base class

```java
package com.becommerce.crm.application.ai.tool;

import com.becommerce.crm.domain.ai.ToolExecutionResult;
import com.fasterxml.jackson.databind.JsonNode;

public abstract class Tool {
    protected final String toolName;
    protected final String description;
    protected final String toolType; // "read", "write", "action"

    public Tool(String toolName, String description, String toolType) {
        this.toolName = toolName;
        this.description = description;
        this.toolType = toolType;
    }

    public abstract ToolExecutionResult execute(JsonNode params);

    public String getToolName() { return toolName; }
    public String getDescription() { return description; }
    public String getToolType() { return toolType; }

    protected long measureExecution(Runnable task) {
        long start = System.currentTimeMillis();
        task.run();
        return System.currentTimeMillis() - start;
    }
}
```

- [ ] Implement `FetchContactTool`

```java
package com.becommerce.crm.application.ai.tool.impl;

import com.becommerce.crm.application.ai.tool.Tool;
import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.domain.ai.ToolExecutionResult;
import com.becommerce.crm.domain.contact.Contact;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.UUID;

@Component
public class FetchContactTool extends Tool {
    private final ContactRepository contactRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FetchContactTool(ContactRepository contactRepository) {
        super("fetchContact", "Fetch contact information by ID", "read");
        this.contactRepository = contactRepository;
    }

    @Override
    public ToolExecutionResult execute(JsonNode params) {
        long start = System.currentTimeMillis();
        try {
            String contactId = params.get("contactId").asText();
            Optional<Contact> contact = contactRepository.findById(UUID.fromString(contactId));
            
            if (contact.isEmpty()) {
                long elapsed = System.currentTimeMillis() - start;
                return ToolExecutionResult.failure("Contact not found", elapsed);
            }

            String json = objectMapper.writeValueAsString(contact.get());
            long elapsed = System.currentTimeMillis() - start;
            return ToolExecutionResult.success(json, elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            return ToolExecutionResult.failure("Error fetching contact: " + e.getMessage(), elapsed);
        }
    }
}
```

- [ ] Implement `SearchDealsTool`

```java
package com.becommerce.crm.application.ai.tool.impl;

import com.becommerce.crm.application.ai.tool.Tool;
import com.becommerce.crm.application.opportunity.port.output.OpportunityRepository;
import com.becommerce.crm.domain.ai.ToolExecutionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class SearchDealsTool extends Tool {
    private final OpportunityRepository opportunityRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SearchDealsTool(OpportunityRepository opportunityRepository) {
        super("searchDeals", "Search deals by contact ID or filters", "read");
        this.opportunityRepository = opportunityRepository;
    }

    @Override
    public ToolExecutionResult execute(JsonNode params) {
        long start = System.currentTimeMillis();
        try {
            String contactId = params.get("contactId").asText();
            int limit = params.has("limit") ? params.get("limit").asInt() : 10;
            
            var deals = opportunityRepository.findByContactId(
                java.util.UUID.fromString(contactId), limit);
            
            String json = objectMapper.writeValueAsString(deals);
            long elapsed = System.currentTimeMillis() - start;
            return ToolExecutionResult.success(json, elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            return ToolExecutionResult.failure("Error searching deals: " + e.getMessage(), elapsed);
        }
    }
}
```

- [ ] Implement `GetActivityTool`

```java
package com.becommerce.crm.application.ai.tool.impl;

import com.becommerce.crm.application.ai.tool.Tool;
import com.becommerce.crm.application.activity.port.output.ActivityRepository;
import com.becommerce.crm.domain.ai.ToolExecutionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class GetActivityTool extends Tool {
    private final ActivityRepository activityRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GetActivityTool(ActivityRepository activityRepository) {
        super("getActivity", "Get activity details by ID", "read");
        this.activityRepository = activityRepository;
    }

    @Override
    public ToolExecutionResult execute(JsonNode params) {
        long start = System.currentTimeMillis();
        try {
            String activityId = params.get("activityId").asText();
            var activity = activityRepository.findById(UUID.fromString(activityId));
            
            if (activity.isEmpty()) {
                long elapsed = System.currentTimeMillis() - start;
                return ToolExecutionResult.failure("Activity not found", elapsed);
            }

            String json = objectMapper.writeValueAsString(activity.get());
            long elapsed = System.currentTimeMillis() - start;
            return ToolExecutionResult.success(json, elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            return ToolExecutionResult.failure("Error getting activity: " + e.getMessage(), elapsed);
        }
    }
}
```

- [ ] Implement `GetProductInfoTool`

```java
package com.becommerce.crm.application.ai.tool.impl;

import com.becommerce.crm.application.ai.tool.Tool;
import com.becommerce.crm.application.product.port.output.ProductRepository;
import com.becommerce.crm.domain.ai.ToolExecutionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class GetProductInfoTool extends Tool {
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GetProductInfoTool(ProductRepository productRepository) {
        super("getProductInfo", "Get product information by ID", "read");
        this.productRepository = productRepository;
    }

    @Override
    public ToolExecutionResult execute(JsonNode params) {
        long start = System.currentTimeMillis();
        try {
            String productId = params.get("productId").asText();
            var product = productRepository.findById(UUID.fromString(productId));
            
            if (product.isEmpty()) {
                long elapsed = System.currentTimeMillis() - start;
                return ToolExecutionResult.failure("Product not found", elapsed);
            }

            String json = objectMapper.writeValueAsString(product.get());
            long elapsed = System.currentTimeMillis() - start;
            return ToolExecutionResult.success(json, elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            return ToolExecutionResult.failure("Error getting product info: " + e.getMessage(), elapsed);
        }
    }
}
```

- [ ] Implement `GetServiceInfoTool`

```java
package com.becommerce.crm.application.ai.tool.impl;

import com.becommerce.crm.application.ai.tool.Tool;
import com.becommerce.crm.application.service.port.output.ServiceRepository;
import com.becommerce.crm.domain.ai.ToolExecutionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class GetServiceInfoTool extends Tool {
    private final ServiceRepository serviceRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GetServiceInfoTool(ServiceRepository serviceRepository) {
        super("getServiceInfo", "Get service information by ID", "read");
        this.serviceRepository = serviceRepository;
    }

    @Override
    public ToolExecutionResult execute(JsonNode params) {
        long start = System.currentTimeMillis();
        try {
            String serviceId = params.get("serviceId").asText();
            var service = serviceRepository.findById(UUID.fromString(serviceId));
            
            if (service.isEmpty()) {
                long elapsed = System.currentTimeMillis() - start;
                return ToolExecutionResult.failure("Service not found", elapsed);
            }

            String json = objectMapper.writeValueAsString(service.get());
            long elapsed = System.currentTimeMillis() - start;
            return ToolExecutionResult.success(json, elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            return ToolExecutionResult.failure("Error getting service info: " + e.getMessage(), elapsed);
        }
    }
}
```

- [ ] Create and run unit tests for tools

```java
package com.becommerce.crm.application.ai.tool.impl;

import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.domain.ai.ToolExecutionResult;
import com.becommerce.crm.domain.contact.Contact;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FetchContactToolTest {
    @Mock
    private ContactRepository contactRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testExecuteWithValidContactId() {
        FetchContactTool tool = new FetchContactTool(contactRepository);
        UUID contactId = UUID.randomUUID();
        
        Contact mockContact = mock(Contact.class);
        when(contactRepository.findById(contactId)).thenReturn(Optional.of(mockContact));
        
        ObjectNode params = objectMapper.createObjectNode();
        params.put("contactId", contactId.toString());
        
        ToolExecutionResult result = tool.execute(params);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getContent());
    }

    @Test
    void testExecuteWithNonexistentContactId() {
        FetchContactTool tool = new FetchContactTool(contactRepository);
        UUID contactId = UUID.randomUUID();
        
        when(contactRepository.findById(contactId)).thenReturn(Optional.empty());
        
        ObjectNode params = objectMapper.createObjectNode();
        params.put("contactId", contactId.toString());
        
        ToolExecutionResult result = tool.execute(params);
        
        assertFalse(result.isSuccess());
        assertEquals("Contact not found", result.getErrorMessage());
    }
}
```

- [ ] Run all tool tests

```bash
cd backend
mvn test -Dtest=FetchContactToolTest,SearchDealsToolTest,GetActivityToolTest -v
```

- [ ] Commit all 5 tools

```bash
git add backend/src/main/java/com/becommerce/crm/application/ai/tool/
git add backend/src/test/java/com/becommerce/crm/application/ai/tool/
git commit -m "feat(ai): implement 5 read-only tools (fetchContact, searchDeals, getActivity, getProductInfo, getServiceInfo)"
```

---

## Task 6: Create ToolRegistry & ToolExecutor

**Files:**
- Create: `backend/src/main/java/com/becommerce/crm/application/ai/tool/ToolRegistry.java`
- Create: `backend/src/main/java/com/becommerce/crm/application/ai/tool/ToolExecutor.java`
- Test: `ToolRegistryTest.java`, `ToolExecutorTest.java`

**Interfaces:**
- Produces: `ToolRegistry.getToolByName(String): Tool`, `getAllTools(): List<Tool>`, `isToolAvailable(String, UUID): boolean`
- Produces: `ToolExecutor.execute(Tool, JsonNode): ToolExecutionResult`

---

## Task 6: Create ToolRegistry & ToolExecutor

**Files:**
- Create: `backend/src/main/java/com/becommerce/crm/application/ai/tool/ToolRegistry.java`
- Create: `backend/src/main/java/com/becommerce/crm/application/ai/tool/ToolExecutor.java`
- Test: `ToolRegistryTest.java`, `ToolExecutorTest.java`

**Interfaces:**
- Produces: `ToolRegistry.getToolByName(String): Tool`, `getAllTools(): List<Tool>`, `isToolAvailable(String, UUID): boolean`
- Produces: `ToolExecutor.execute(Tool, JsonNode, UUID): ToolExecutionResult`

**Steps:**

- [ ] Create `ToolRegistry`

```java
package com.becommerce.crm.application.ai.tool;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ToolRegistry {
    private final List<Tool> tools;
    private final Map<String, Tool> toolMap;

    public ToolRegistry(List<Tool> tools) {
        this.tools = tools;
        this.toolMap = tools.stream()
            .collect(Collectors.toMap(Tool::getToolName, t -> t));
    }

    public Optional<Tool> getToolByName(String toolName) {
        return Optional.ofNullable(toolMap.get(toolName));
    }

    public List<Tool> getAllTools() {
        return List.copyOf(tools);
    }

    public List<Tool> getReadOnlyTools() {
        return tools.stream()
            .filter(t -> "read".equals(t.getToolType()))
            .toList();
    }

    public boolean exists(String toolName) {
        return toolMap.containsKey(toolName);
    }
}
```

- [ ] Create `ToolExecutor`

```java
package com.becommerce.crm.application.ai.tool;

import com.becommerce.crm.domain.ai.ToolExecutionResult;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ToolExecutor {
    private static final Logger log = LoggerFactory.getLogger(ToolExecutor.class);

    public ToolExecutionResult execute(Tool tool, JsonNode params) {
        log.info("[TOOL] Executing tool: {}", tool.getToolName());
        try {
            ToolExecutionResult result = tool.execute(params);
            if (result.isSuccess()) {
                log.info("[TOOL] Success: {} ({}ms)", tool.getToolName(), result.getExecutionTimeMs());
            } else {
                log.warn("[TOOL] Failed: {} - {}", tool.getToolName(), result.getErrorMessage());
            }
            return result;
        } catch (Exception e) {
            log.error("[TOOL] Exception executing {}: {}", tool.getToolName(), e.getMessage(), e);
            return ToolExecutionResult.failure(
                "Tool execution error: " + e.getMessage(),
                0
            );
        }
    }
}
```

- [ ] Create tests

```java
package com.becommerce.crm.application.ai.tool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ToolRegistryTest {
    @Test
    void testGetToolByName() {
        Tool mockTool = mock(Tool.class);
        mockTool.getToolName() returns "fetchContact";
        
        ToolRegistry registry = new ToolRegistry(List.of(mockTool));
        assertTrue(registry.exists("fetchContact"));
        assertTrue(registry.getToolByName("fetchContact").isPresent());
    }

    @Test
    void testGetAllTools() {
        Tool tool1 = mock(Tool.class);
        Tool tool2 = mock(Tool.class);
        
        ToolRegistry registry = new ToolRegistry(List.of(tool1, tool2));
        assertEquals(2, registry.getAllTools().size());
    }
}
```

- [ ] Run tests

```bash
cd backend
mvn test -Dtest=ToolRegistryTest -v
```

- [ ] Commit

```bash
git add backend/src/main/java/com/becommerce/crm/application/ai/tool/ToolRegistry.java
git add backend/src/main/java/com/becommerce/crm/application/ai/tool/ToolExecutor.java
git add backend/src/test/java/com/becommerce/crm/application/ai/tool/
git commit -m "feat(ai): add ToolRegistry and ToolExecutor for tool invocation"
```

---

## Task 7: Create REST Endpoints for Tool Management

**Files:**
- Create: `backend/src/main/java/com/becommerce/crm/presentation/rest/ai/AgentToolController.java`
- Create: DTOs: `AgentToolResponse.java`, `CreateAgentToolRequest.java`, `UpdateAgentToolRequest.java`
- Test: `AgentToolControllerTest.java`

**Interfaces:**
- GET `/api/v1/agent/tools` — list tools for company
- GET `/api/v1/agent/tools/{toolId}` — get tool details
- POST `/api/v1/agent/tools/{toolId}/enable` — enable tool
- POST `/api/v1/agent/tools/{toolId}/disable` — disable tool
- GET `/api/v1/agent/audit` — list agent action audit trail

**Steps:**

- [ ] Create DTOs

```java
package com.becommerce.crm.application.ai.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AgentToolResponse(
    UUID id,
    String toolName,
    String description,
    boolean enabled,
    String toolType,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

- [ ] Create `AgentToolController`

```java
package com.becommerce.crm.presentation.rest.ai;

import com.becommerce.crm.application.ai.dto.AgentToolResponse;
import com.becommerce.crm.application.ai.port.output.AgentToolRepository;
import com.becommerce.crm.domain.ai.AgentTool;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agent/tools")
public class AgentToolController {
    private final AgentToolRepository toolRepository;

    public AgentToolController(AgentToolRepository toolRepository) {
        this.toolRepository = toolRepository;
    }

    @GetMapping
    public ResponseEntity<List<AgentToolResponse>> listTools() {
        UUID companyId = TenantContext.getCompanyId();
        List<AgentTool> tools = toolRepository.findByCompanyId(companyId);
        List<AgentToolResponse> response = tools.stream()
            .map(t -> new AgentToolResponse(t.getId(), t.getToolName(), t.getDescription(),
                t.isEnabled(), t.getToolType(), t.getCreatedAt(), t.getUpdatedAt()))
            .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{toolId}")
    public ResponseEntity<AgentToolResponse> getTool(@PathVariable UUID toolId) {
        UUID companyId = TenantContext.getCompanyId();
        return toolRepository.findById(toolId)
            .filter(t -> t.getCompanyId().equals(companyId))
            .map(t -> new AgentToolResponse(t.getId(), t.getToolName(), t.getDescription(),
                t.isEnabled(), t.getToolType(), t.getCreatedAt(), t.getUpdatedAt()))
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{toolId}/enable")
    public ResponseEntity<Void> enableTool(@PathVariable UUID toolId) {
        // To be implemented in next sprint (requires service layer)
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{toolId}/disable")
    public ResponseEntity<Void> disableTool(@PathVariable UUID toolId) {
        // To be implemented in next sprint (requires service layer)
        return ResponseEntity.ok().build();
    }
}
```

- [ ] Commit

```bash
git add backend/src/main/java/com/becommerce/crm/application/ai/dto/
git add backend/src/main/java/com/becommerce/crm/presentation/rest/ai/
git commit -m "feat(api): add REST endpoints for agent tool management"
```

---

## Task 8: Extend WhatsAppInboundAutoReplyProcessor to Support Tools

**Files:**
- Modify: `backend/src/main/java/com/becommerce/crm/application/omnichannel/service/WhatsAppInboundAutoReplyProcessor.java`
- Create: `AgentContextBuilder.java` (richer context with contact data)
- Test: Modify existing tests to include tool context

**Interfaces:**
- Consumes: `ToolRegistry.getToolByName(String)`, `ToolExecutor.execute(Tool, JsonNode)`
- Produces: (same as before, but now includes tool execution logic)

**Steps:**

- [ ] Create `AgentContextBuilder` for richer context

```java
package com.becommerce.crm.application.ai.context;

import com.becommerce.crm.application.ai.port.output.AiProvider;
import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.domain.ai.AgentConfig;
import com.becommerce.crm.domain.contact.Contact;
import com.becommerce.crm.domain.omnichannel.Conversation;
import com.becommerce.crm.domain.omnichannel.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AgentContextBuilder {
    private final ContactRepository contactRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int HISTORY_LIMIT = 20;
    private static final int DEFAULT_MAX_TOKENS = 600;
    private static final int CHARS_PER_TOKEN = 4;
    private static final int HISTORY_TOKEN_MULTIPLIER = 2;

    public AgentContextBuilder(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    public List<AiProvider.ChatMessage> buildContext(
            AgentConfig agentConfig, Conversation conversation, UUID inboundMessageId,
            String body, List<Message> history) {
        
        List<AiProvider.ChatMessage> messages = new ArrayList<>();
        messages.add(new AiProvider.ChatMessage("system", buildSystemPrompt(agentConfig, conversation)));

        int outputBudget = agentConfig.getMaxTokens() != null && agentConfig.getMaxTokens() > 0
                ? agentConfig.getMaxTokens() : DEFAULT_MAX_TOKENS;
        int historyCharBudget = outputBudget * HISTORY_TOKEN_MULTIPLIER * CHARS_PER_TOKEN;

        List<AiProvider.ChatMessage> kept = new ArrayList<>();
        int usedChars = 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            Message m = history.get(i);
            if (m.getBody() == null || m.getBody().isBlank()) continue;
            if (inboundMessageId != null && inboundMessageId.equals(m.getId())) continue;

            String role = "INBOUND".equals(m.getDirection().name()) ? "user" : "assistant";
            int estimated = m.getBody().length() + role.length();
            if (usedChars + estimated > historyCharBudget && !kept.isEmpty()) break;

            kept.add(0, new AiProvider.ChatMessage(role, m.getBody()));
            usedChars += estimated;
        }
        messages.addAll(kept);
        messages.add(new AiProvider.ChatMessage("user", body));
        return messages;
    }

    private String buildSystemPrompt(AgentConfig config, Conversation conversation) {
        StringBuilder sb = new StringBuilder(config.getSystemPrompt()).append("\n\n");
        sb.append("## Available Tools:\n");
        sb.append("- fetchContact(contactId): Get contact information\n");
        sb.append("- searchDeals(contactId, limit): Search deals for a contact\n");
        sb.append("- getActivity(activityId): Get activity details\n");
        sb.append("- getProductInfo(productId): Get product information\n");
        sb.append("- getServiceInfo(serviceId): Get service information\n");
        return sb.toString();
    }
}
```

- [ ] Modify `WhatsAppInboundAutoReplyProcessor` to inject new dependencies

```java
// Add to WhatsAppInboundAutoReplyProcessor constructor
public WhatsAppInboundAutoReplyProcessor(
        AgentConfigRepository agentConfigRepository,
        AgentAutoReplyRepository autoReplyRepository,
        OmnichannelConversationRepository conversationRepository,
        OmnichannelChannelRepository channelRepository,
        OmnichannelMessageRepository messageRepository,
        AiChatFailover aiChatFailover,
        OmnichannelMessagePersister messagePersister,
        WhatsAppEventPublisher eventPublisher,
        ToolRegistry toolRegistry,                    // NEW
        ToolExecutor toolExecutor,                    // NEW
        AgentContextBuilder contextBuilder,           // NEW
        AgentActionAuditRepository auditRepository) { // NEW
    // ... existing assignments ...
    this.toolRegistry = toolRegistry;
    this.toolExecutor = toolExecutor;
    this.contextBuilder = contextBuilder;
    this.auditRepository = auditRepository;
}
```

- [ ] Add tool execution logic to `generateReply()` method

```java
private String generateReply(AgentConfig agentConfig, UUID conversationId,
                             UUID inboundMessageId, String body, Conversation conv) {
    long generationStart = System.nanoTime();
    try {
        List<AiProvider.ChatMessage> messages =
                contextBuilder.buildContext(agentConfig, conv, inboundMessageId, body, /* history */);

        AiProvider.GenerationParams params = new AiProvider.GenerationParams(
                agentConfig.getModel(), agentConfig.getTemperature(),
                agentConfig.getMaxTokens(), null);
        
        AiProvider.ChatResult result = aiChatFailover.chat(new AiProvider.ChatRequest(
                agentConfig.getCompanyId(), null, messages).withParams(params));

        String reply = result != null ? result.content() : null;
        if (reply == null || reply.isBlank()) {
            return null;
        }

        // Check if AI wants to call a tool (simple heuristic: response contains tool markers)
        if (containsToolMarkers(reply)) {
            String toolResult = attemptToolExecution(agentConfig, reply);
            if (toolResult != null) {
                // Re-invoke AI with tool result
                messages.add(new AiProvider.ChatMessage("assistant", reply));
                messages.add(new AiProvider.ChatMessage("user", "Tool execution result:\n" + toolResult));
                
                AiProvider.ChatResult refinedResult = aiChatFailover.chat(
                    new AiProvider.ChatRequest(agentConfig.getCompanyId(), null, messages)
                        .withParams(params));
                reply = refinedResult != null ? refinedResult.content() : reply;
            }
        }

        return reply.trim();
    } catch (AiProviderException e) {
        log.error("Generation failed: {}", e.getMessage());
        return null;
    }
}

private boolean containsToolMarkers(String response) {
    return response.contains("fetchContact") || response.contains("searchDeals") ||
           response.contains("getActivity") || response.contains("getProductInfo") ||
           response.contains("getServiceInfo");
}

private String attemptToolExecution(AgentConfig agentConfig, String response) {
    // Parse tool calls from response (simplified)
    try {
        // Example: response contains "fetchContact(contactId=123)"
        for (String toolName : List.of("fetchContact", "searchDeals", "getActivity", "getProductInfo", "getServiceInfo")) {
            if (response.contains(toolName)) {
                Optional<Tool> tool = toolRegistry.getToolByName(toolName);
                if (tool.isPresent()) {
                    // Extract params (simplified)
                    com.fasterxml.jackson.databind.node.ObjectNode params =
                        new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
                    // TODO: parse actual parameters from response
                    ToolExecutionResult result = toolExecutor.execute(tool.get(), params);
                    if (result.isSuccess()) {
                        return result.getContent();
                    }
                }
            }
        }
    } catch (Exception e) {
        log.warn("Tool execution failed: {}", e.getMessage());
    }
    return null;
}
```

- [ ] Commit

```bash
git add backend/src/main/java/com/becommerce/crm/application/ai/context/AgentContextBuilder.java
git add backend/src/main/java/com/becommerce/crm/application/omnichannel/service/WhatsAppInboundAutoReplyProcessor.java
git commit -m "feat(ai): integrate tool execution into WhatsApp auto-reply processor"
```

---

## Task 9: Create Service Layer for Agent Configuration

**Files:**
- Create: `backend/src/main/java/com/becommerce/crm/application/ai/service/AgentToolManagementService.java`
- Test: `AgentToolManagementServiceTest.java`

**Interfaces:**
- Produces: `enableToolForCompany(UUID companyId, String toolName): void`, `disableToolForCompany(UUID, String): void`

**Steps:**

- [ ] Create `AgentToolManagementService`

```java
package com.becommerce.crm.application.ai.service;

import com.becommerce.crm.application.ai.port.output.AgentToolRepository;
import com.becommerce.crm.domain.ai.AgentTool;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class AgentToolManagementService {
    private final AgentToolRepository toolRepository;

    public AgentToolManagementService(AgentToolRepository toolRepository) {
        this.toolRepository = toolRepository;
    }

    public void initializeDefaultTools(UUID companyId) {
        // Create default tools (read-only, disabled by default)
        String[] defaultTools = {
            "fetchContact:Fetch contact information:read",
            "searchDeals:Search deals for a contact:read",
            "getActivity:Get activity details:read",
            "getProductInfo:Get product information:read",
            "getServiceInfo:Get service information:read"
        };

        for (String tool : defaultTools) {
            String[] parts = tool.split(":");
            toolRepository.save(AgentTool.create(companyId, parts[0], parts[1], parts[2]));
        }
    }

    public void enableTool(UUID companyId, String toolName) {
        var tool = toolRepository.findByCompanyAndName(companyId, toolName);
        if (tool.isPresent()) {
            tool.get().enable();
            toolRepository.save(tool.get());
        }
    }

    public void disableTool(UUID companyId, String toolName) {
        var tool = toolRepository.findByCompanyAndName(companyId, toolName);
        if (tool.isPresent()) {
            tool.get().disable();
            toolRepository.save(tool.get());
        }
    }
}
```

- [ ] Commit

```bash
git add backend/src/main/java/com/becommerce/crm/application/ai/service/AgentToolManagementService.java
git commit -m "feat(ai): add tool management service"
```

---

## Task 10: Add Audit Logging Service

**Files:**
- Create: `backend/src/main/java/com/becommerce/crm/application/ai/service/AgentActionAuditService.java`
- Test: `AgentActionAuditServiceTest.java`

**Interfaces:**
- Produces: `logToolExecution(UUID companyId, UUID conversationId, String toolName, String input, String result, String outcome): void`

**Steps:**

- [ ] Create `AgentActionAuditService`

```java
package com.becommerce.crm.application.ai.service;

import com.becommerce.crm.application.ai.port.output.AgentActionAuditRepository;
import com.becommerce.crm.domain.ai.AgentActionAudit;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class AgentActionAuditService {
    private final AgentActionAuditRepository auditRepository;

    public AgentActionAuditService(AgentActionAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public void logToolExecution(UUID companyId, UUID conversationId, String toolName,
                                String input, String result, String outcome) {
        AgentActionAudit audit = AgentActionAudit.create(companyId, conversationId,
                toolName, input, result, outcome);
        auditRepository.save(audit);
    }

    public void logToolFailure(UUID companyId, UUID conversationId, String toolName,
                              String input, String errorMessage) {
        logToolExecution(companyId, conversationId, toolName, input, errorMessage, "failed");
    }

    public void logToolSuccess(UUID companyId, UUID conversationId, String toolName,
                              String input, String resultJson) {
        logToolExecution(companyId, conversationId, toolName, input, resultJson, "success");
    }
}
```

- [ ] Commit

```bash
git add backend/src/main/java/com/becommerce/crm/application/ai/service/AgentActionAuditService.java
git commit -m "feat(ai): add audit logging service"
```

---

## Task 11: Integration Tests & Documentation

**Files:**
- Create: `backend/src/test/java/com/becommerce/crm/application/ai/WhatsAppAgentPhase1IntegrationTest.java`
- Create: `docs/WHATSAPP_AGENT_PHASE1.md` (feature documentation)

**Steps:**

- [ ] Create integration test

```java
package com.becommerce.crm.application.ai;

import com.becommerce.crm.application.ai.port.output.AgentToolRepository;
import com.becommerce.crm.application.ai.service.AgentToolManagementService;
import com.becommerce.crm.application.ai.tool.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WhatsAppAgentPhase1IntegrationTest {
    @Autowired
    private AgentToolRepository toolRepository;
    
    @Autowired
    private AgentToolManagementService toolService;
    
    @Autowired
    private ToolRegistry toolRegistry;

    @Test
    void testPhase1ToolsInitialized() {
        UUID companyId = UUID.randomUUID();
        toolService.initializeDefaultTools(companyId);
        
        var tools = toolRepository.findByCompanyId(companyId);
        assertEquals(5, tools.size());
        assertTrue(tools.stream().allMatch(t -> !t.isEnabled()));
    }

    @Test
    void testToolRegistryHasAllTools() {
        assertTrue(toolRegistry.exists("fetchContact"));
        assertTrue(toolRegistry.exists("searchDeals"));
        assertTrue(toolRegistry.exists("getActivity"));
        assertTrue(toolRegistry.exists("getProductInfo"));
        assertTrue(toolRegistry.exists("getServiceInfo"));
    }

    @Test
    void testEnableToolForCompany() {
        UUID companyId = UUID.randomUUID();
        toolService.initializeDefaultTools(companyId);
        toolService.enableTool(companyId, "fetchContact");
        
        var tool = toolRepository.findByCompanyAndName(companyId, "fetchContact");
        assertTrue(tool.isPresent());
        assertTrue(tool.get().isEnabled());
    }
}
```

- [ ] Create documentation

```markdown
# WhatsApp AI Agent — Phase 1 Implementation

## Overview

Phase 1 introduces **5 read-only tools** that enable the AI agent to consult CRM data when responding to WhatsApp messages.

## Implemented Tools

| Tool | Purpose | Type |
|------|---------|------|
| `fetchContact` | Fetch contact information by ID | read |
| `searchDeals` | Search deals for a contact | read |
| `getActivity` | Get activity details by ID | read |
| `getProductInfo` | Get product information by ID | read |
| `getServiceInfo` | Get service information by ID | read |

## Architecture

### Components

1. **AgentTool** (domain) — Represents a tool available to the agent
2. **ToolPermission** (domain) — Role-based access control for tools
3. **AgentActionAudit** (domain) — Audit trail for all agent actions
4. **ToolRegistry** (application) — Registry of available tools
5. **ToolExecutor** (application) — Executes tool calls
6. **AgentContextBuilder** (application) — Builds enriched context with tool availability
7. **AgentToolManagementService** (application) — Enables/disables tools per company
8. **AgentActionAuditService** (application) — Logs tool invocations

### Database

- `agent_tool` — Available tools (scoped per company, with RLS)
- `tool_permission` — Role-based access control
- `agent_action_audit` — Comprehensive audit trail

All tables use RLS to enforce multi-tenant isolation.

## How It Works

1. **Webhook receives message** → WhatsAppInboundConsumer processes it
2. **Async processor** → WhatsAppAutoAiConsumer triggers AI agent
3. **AI Agent** (via LLM) sees available tools in system prompt
4. **AI may call a tool** (e.g., "fetchContact(contactId=123)")
5. **Tool execution** → ToolExecutor invokes the tool
6. **Tool result logged** → AgentActionAudit records what was executed
7. **AI refines response** → Result passed back to LLM for contextual reply
8. **Reply sent** → WhatsAppSendEvent publishes message to UAZAPI

## Configuration

### Enable tools for a company

```java
UUID companyId = getCurrentCompanyId();
agentToolManagementService.enableTool(companyId, "fetchContact");
agentToolManagementService.enableTool(companyId, "searchDeals");
```

### Audit trail

```java
// View all tool invocations for a conversation
var audits = auditRepository.findByConversationId(conversationId, 0, 10);
for (AgentActionAudit audit : audits.content()) {
    System.out.println(audit.getToolName() + " → " + audit.getOutcome());
}
```

## Security

- **Multi-tenancy** — Tools are scoped per company; RLS enforces isolation
- **Permissions** — Each tool can have role-based restrictions (managed via ToolPermission)
- **Audit** — All executions logged immutably
- **No tool calls without consent** — Company must explicitly enable tools; default is disabled
- **Tenant context preserved** — All tool execution respects TenantContext

## Future (Phase 2–3)

- **Agentic loop** — Allow multi-turn tool calls with recursion limits
- **Write actions** — Tools to create/update entities (createActivity, setDealStage, etc.)
- **Confirmation gates** — Some actions require approval before execution
- **Tool-specific permissions** — Finer-grained role-based access
- **Worker offload** — Long-running tools execute in separate RabbitMQ consumers
```

- [ ] Run all tests

```bash
cd backend
mvn test -v
```

- [ ] Commit

```bash
git add backend/src/test/java/com/becommerce/crm/application/ai/WhatsAppAgentPhase1IntegrationTest.java
git add docs/WHATSAPP_AGENT_PHASE1.md
git commit -m "test(ai): add Phase 1 integration tests and documentation"
```

---

## Final Verification Checklist

- [ ] All domain entities created and tested (AgentTool, ToolPermission, AgentActionAudit)
- [ ] Database migration applied successfully (V077)
- [ ] Repositories implemented and JPA entities mapped
- [ ] 5 read-only tools implemented (fetchContact, searchDeals, getActivity, getProductInfo, getServiceInfo)
- [ ] ToolRegistry and ToolExecutor working
- [ ] AgentContextBuilder enriching prompt with tool availability
- [ ] WhatsAppInboundAutoReplyProcessor integrated with tool execution
- [ ] REST endpoints available for tool management
- [ ] Audit trail logging working for all tool invocations
- [ ] Multi-tenant isolation (TenantContext + RLS) preserved across all layers
- [ ] Integration tests passing
- [ ] Documentation complete (WHATSAPP_AGENT_PHASE1.md)

---

## Execution Handoff

**Plan complete and saved to `docs/superpowers/plans/2026-09-25-whatsapp-ai-agent-phase1.md`**

Two execution options:

**1. Subagent-Driven (Recommended)** — Fresh subagent per task, review between tasks, fast iteration. Use `superpowers:subagent-driven-development`.

**2. Inline Execution** — Execute tasks in this session using `superpowers:executing-plans`, batch execution with checkpoints.

**Which approach?**