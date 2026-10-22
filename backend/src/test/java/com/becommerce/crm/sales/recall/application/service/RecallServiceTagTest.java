package com.becommerce.crm.sales.recall.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.sales.recall.application.dto.CreateRecallRequest;
import com.becommerce.crm.sales.recall.application.dto.RecallResponse;
import com.becommerce.crm.sales.recall.application.port.out.RecallRepository;
import com.becommerce.crm.sales.recall.application.port.out.RecallSuggestionDismissalRepository;
import com.becommerce.crm.sales.recall.domain.Recall;
import com.becommerce.crm.sales.recall.domain.RecallDueUnit;
import com.becommerce.crm.sales.tag.application.dto.TagResponse;
import com.becommerce.crm.sales.tag.application.port.in.TagUseCase;
import com.becommerce.crm.sales.tag.domain.exception.TagNotFoundException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecallServiceTagTest {

    private final RecallRepository repository = mock(RecallRepository.class);
    private final TagUseCase tags = mock(TagUseCase.class);
    private final RecallService service = new RecallService(repository,
            mock(RecallSuggestionDismissalRepository.class), mock(TenantAuditRecorder.class), tags);
    private final UUID companyId = UUID.randomUUID();

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private CreateRecallRequest request(UUID tagId) {
        return new CreateRecallRequest(UUID.randomUUID(), "11999998888", UUID.randomUUID(), null, 6,
                RecallDueUnit.MONTHS, tagId);
    }

    @Test
    void linksRegisteredTagAndKeepsTenantForSave() {
        UUID tagId = UUID.randomUUID();
        when(tags.getById(companyId, tagId)).thenAnswer(inv -> {
            TenantContext.clear(); // como o TagService real
            return new TagResponse(tagId, companyId, "Ortodontia", "#22c55e", Instant.now(), Instant.now());
        });
        AtomicReference<UUID> tenantOnSave = new AtomicReference<>();
        when(repository.save(any(Recall.class))).thenAnswer(inv -> {
            tenantOnSave.set(TenantContext.getCompanyId());
            return inv.getArgument(0);
        });

        RecallResponse response = service.create(companyId, request(tagId), null);

        assertEquals(tagId, response.tagId());
        assertEquals("Ortodontia", response.tag());
        assertEquals(companyId, tenantOnSave.get());
    }

    @Test
    void rejectsTagFromAnotherCompany() {
        UUID foreignTag = UUID.randomUUID();
        when(tags.getById(companyId, foreignTag)).thenThrow(new TagNotFoundException("Marcador", foreignTag));

        assertThrows(TagNotFoundException.class, () -> service.create(companyId, request(foreignTag), null));
        verify(repository, never()).save(any());
    }
}
