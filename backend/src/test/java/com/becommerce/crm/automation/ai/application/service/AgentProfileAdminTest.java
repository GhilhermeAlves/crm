package com.becommerce.crm.automation.ai.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.automation.ai.application.dto.AgentBehaviorDto;
import com.becommerce.crm.automation.ai.application.dto.AgentConfigRequest;
import com.becommerce.crm.automation.ai.application.dto.AgentConfigResponse;
import com.becommerce.crm.automation.ai.application.dto.AgentIdentityDto;
import com.becommerce.crm.automation.ai.application.dto.AgentMemoryRequest;
import com.becommerce.crm.automation.ai.application.memory.AgentMemoryAdminService;
import com.becommerce.crm.automation.ai.application.memory.AgentMemoryService;
import com.becommerce.crm.automation.ai.application.port.output.AgentConfigRepository;
import com.becommerce.crm.automation.ai.domain.AgentBehavior;
import com.becommerce.crm.automation.ai.domain.AgentConfig;
import com.becommerce.crm.automation.ai.domain.AgentIdentity;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.masterdata.contact.domain.Contact;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** API do agente com a nova arquitetura (V088) e gestão de memórias pela equipe. */
class AgentProfileAdminTest {

    private static final UUID COMPANY_ID = UUID.randomUUID();

    private final AgentConfigRepository repository = mock(AgentConfigRepository.class);
    private final AgentConfigAdminService service = new AgentConfigAdminService(repository,
            mock(TenantAuditRecorder.class));

    @AfterEach
    void clean() {
        TenantContext.clear();
    }

    private AgentConfigRequest structured() {
        return new AgentConfigRequest(true, true, "Prompt legado", null, 0.4, 1000, 5, 500, "MIRROR",
                new AgentIdentityDto("Ana Laura", "Assistente virtual", "Você é Ana Laura."),
                new AgentBehaviorDto("Atender e agendar", "Acolhedor", List.of("Não inventar"), List.of("Confirmar data")),
                true, true);
    }

    @Test
    void create_withStructuredProfile_returnsAllSections() {
        when(repository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AgentConfigResponse response = service.update(COMPANY_ID, structured());

        assertEquals("Ana Laura", response.identity().name());
        assertEquals(List.of("Não inventar"), response.behavior().rules());
        assertEquals(1000, response.modelConfig().maxTokens());
        assertEquals(5, response.conversation().cooldownMinutes());
        assertTrue(response.conversation().memory().enabled());
        assertTrue(response.tools().humanTransferEnabled());
        assertFalse(response.usesLegacyPrompt());
        assertEquals("Prompt legado", response.systemPrompt(), "o legado é preservado, não apagado");
    }

    @Test
    void legacyClient_withFlatFieldsOnly_keepsExistingProfile() {
        AgentConfig existing = AgentConfig.reconstitute(UUID.randomUUID(), COMPANY_ID, true, true, "p", null, null,
                        null, 60, 1000, LocalDateTime.now(), LocalDateTime.now())
                .withProfile(new AgentIdentity("Ana", null, null), new AgentBehavior("Obj", null, List.of(), List.of()),
                        true, false);
        when(repository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AgentConfigResponse response = service.update(COMPANY_ID,
                new AgentConfigRequest(true, false, "p2", null, null, null, 30, 800));

        assertEquals("Ana", response.identity().name());
        assertEquals("Obj", response.behavior().objective());
        assertTrue(response.conversation().memory().enabled());
        assertFalse(response.allowAutoReply());
    }

    @Test
    void get_legacyAgent_flagsLegacyPrompt() {
        when(repository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(AgentConfig.reconstitute(
                UUID.randomUUID(), COMPANY_ID, true, true, "Prompt antigo", null, null, null, 60, 1000,
                LocalDateTime.now(), LocalDateTime.now())));

        AgentConfigResponse response = service.get(COMPANY_ID);

        assertTrue(response.usesLegacyPrompt());
        assertNull(response.identity().name());
        assertFalse(response.conversation().memory().enabled());
    }

    @Test
    void memoryAdmin_rejectsContactFromAnotherCompany() {
        AgentMemoryService memoryService = mock(AgentMemoryService.class);
        ContactRepository contacts = mock(ContactRepository.class);
        UUID foreignContact = UUID.randomUUID();
        Contact contact = mock(Contact.class);
        when(contact.getCompanyId()).thenReturn(UUID.randomUUID());
        when(contacts.findById(foreignContact)).thenReturn(Optional.of(contact));
        AgentMemoryAdminService admin = new AgentMemoryAdminService(memoryService, repository, contacts);

        assertThrows(AgentMemoryAdminService.MemoryRequestException.class, () -> admin.list(COMPANY_ID, foreignContact));
        assertThrows(AgentMemoryAdminService.MemoryRequestException.class, () -> admin.create(COMPANY_ID,
                new AgentMemoryRequest(foreignContact, "FACT", "Já fez avaliação", 3)));
        verify(memoryService, never()).listByContact(any(), any(), org.mockito.ArgumentMatchers.anyInt());
        assertNull(TenantContext.getCompanyId(), "TenantContext sempre limpo");
    }
}
