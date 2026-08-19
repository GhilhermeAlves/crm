package com.becommerce.crm.application.ai.tool;

import com.becommerce.crm.application.ai.context.AiPermissionContext;
import com.becommerce.crm.application.contact.port.input.ContactUseCase;
import com.becommerce.crm.application.ai.tool.tools.ContactTool;
import com.becommerce.crm.application.ai.tool.tools.CustomerTool;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiToolRegistryTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private final ContactUseCase contactUseCase = mock(ContactUseCase.class);

    private AiToolRegistry registry() {
        return new AiToolRegistry(List.of(
                new ContactTool(contactUseCase),
                new CustomerTool(contactUseCase)));
    }

    private AiToolContext ctx(List<String> permissions) {
        return new AiToolContext(companyId, userId, new AiPermissionContext(permissions));
    }

    @Test
    void shouldFindExistingTool() {
        assertTrue(registry().find("get_contact").isPresent());
    }

    @Test
    void shouldNotFindUnknownTool() {
        assertTrue(registry().find("delete_everything").isEmpty());
    }

    @Test
    void shouldListTools() {
        var names = registry().list().stream().map(AiTool::name).toList();
        assertTrue(names.contains("get_contact"));
        assertTrue(names.contains("get_customer"));
    }

    @Test
    void shouldAllowWhenPermissionPresent() {
        var c = mock(com.becommerce.crm.application.contact.dto.ContactResponse.class);
        when(contactUseCase.getById(any(), any())).thenReturn(c);
        var result = registry().execute("get_contact", ctx(List.of("contact:read")),
                Map.of("contactId", UUID.randomUUID().toString()));
        assertTrue(result.success());
    }

    @Test
    void shouldDenyWhenPermissionMissing() {
        var result = registry().execute("get_contact", ctx(List.of("lead:read")),
                Map.of("contactId", UUID.randomUUID().toString()));
        assertFalse(result.success());
        assertTrue(result.error().contains("permissão"));
    }

    @Test
    void shouldDenyUnknownTool() {
        var result = registry().execute("nope", ctx(List.of("contact:read")), Map.of());
        assertFalse(result.success());
        assertTrue(result.error().contains("desconhecida"));
    }

    @Test
    void shouldReportToolFailureAsFail() {
        // contactUseCase returns null -> fake an unexpected error path
        when(contactUseCase.getById(any(), any())).thenThrow(new RuntimeException("boom"));
        var result = registry().execute("get_contact", ctx(List.of("contact:read")),
                Map.of("contactId", UUID.randomUUID().toString()));
        assertFalse(result.success());
        assertTrue(result.error().contains("boom"));
    }
}