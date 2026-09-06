package com.becommerce.crm.application.ai.service;

import com.becommerce.crm.application.ai.dto.AgentConfigRequest;
import com.becommerce.crm.application.ai.dto.AgentConfigResponse;
import com.becommerce.crm.application.ai.port.output.AgentConfigRepository;
import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.domain.ai.AgentConfig;
import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.audit.AuditModule;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentConfigAdminServiceTest {

    private static final UUID COMPANY_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Mock AgentConfigRepository agentConfigRepository;
    @Mock TenantAuditRecorder auditor;

    @InjectMocks AgentConfigAdminService service;

    @BeforeEach
    @AfterEach
    void cleanTenant() {
        TenantContext.clear();
    }

    private AgentConfigRequest request(boolean aiEnabled, boolean allowAutoReply) {
        return new AgentConfigRequest(aiEnabled, allowAutoReply, "Você responde como Léo.",
                "gpt-4o", 0.7, 300, 45, 900);
    }

    @Test
    void get_whenNoConfig_shouldReturnSafeDefaultsWithNullId() {
        when(agentConfigRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());

        AgentConfigResponse response = service.get(COMPANY_ID);

        assertNull(response.id());
        assertFalse(response.aiEnabled());
        assertFalse(response.allowAutoReply());
        assertNull(response.systemPrompt());
        assertEquals(60, response.cooldownMinutes());
        assertEquals(1000, response.maxChars());
    }

    @Test
    void get_whenConfigured_shouldReturnStoredValues() {
        AgentConfig config = AgentConfig.reconstitute(UUID.randomUUID(), COMPANY_ID, true, true,
                "prompt", "gpt-4o", 0.5, 400, 30, 800,
                LocalDateTime.now(), LocalDateTime.now());
        when(agentConfigRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(config));

        AgentConfigResponse response = service.get(COMPANY_ID);

        assertEquals(true, response.aiEnabled());
        assertEquals(true, response.allowAutoReply());
        assertEquals("gpt-4o", response.model());
        assertEquals(800, response.maxChars());
        assertEquals(config.getId(), response.id());
    }

    @Test
    void update_whenNoConfig_shouldCreateAndAuditCreate() {
        when(agentConfigRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());
        when(agentConfigRepository.save(any(AgentConfig.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AgentConfigResponse response = service.update(COMPANY_ID, request(true, true));

        assertTrue(response.aiEnabled());
        assertTrue(response.allowAutoReply());
        assertEquals("gpt-4o", response.model());
        verify(agentConfigRepository).save(any(AgentConfig.class));
        verify(auditor).record(eq(COMPANY_ID), eq(AuditAction.CREATE), eq(AuditModule.AI),
                eq("AgentConfig"), eq(COMPANY_ID.toString()), any(), isNull(), isNull());
        // Tenant definido e limpo em todos os caminhos.
        assertEquals(null, TenantContext.getCompanyId());
    }

    @Test
    void update_whenConfigExists_shouldPreserveIdAndAuditUpdate() {
        UUID id = UUID.randomUUID();
        AgentConfig existing = AgentConfig.reconstitute(id, COMPANY_ID, false, false,
                "antigo", null, null, null, 60, 1000,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1));
        when(agentConfigRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(existing));
        when(agentConfigRepository.save(any(AgentConfig.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AgentConfigResponse response = service.update(COMPANY_ID, request(true, true));

        assertEquals(id, response.id());
        assertEquals(true, response.aiEnabled());
        assertTrue(response.allowAutoReply());
        assertEquals("gpt-4o", response.model());
        assertEquals(45, response.cooldownMinutes());
        assertEquals(900, response.maxChars());
        verify(agentConfigRepository).save(any(AgentConfig.class));
        verify(auditor).record(eq(COMPANY_ID), eq(AuditAction.UPDATE), eq(AuditModule.AI),
                eq("AgentConfig"), eq(COMPANY_ID.toString()), any(), isNull(), isNull());
    }

    @Test
    void update_whenAuditFails_shouldNotFailTheOperation() {
        when(agentConfigRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());
        when(agentConfigRepository.save(any(AgentConfig.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("audit down"))
                .when(auditor).record(any(), any(), any(), any(), any(), any(), any(), any());

        AgentConfigResponse response = service.update(COMPANY_ID, request(false, false));

        assertFalse(response.aiEnabled());
        verify(agentConfigRepository).save(any(AgentConfig.class));
        assertEquals(null, TenantContext.getCompanyId());
    }
}