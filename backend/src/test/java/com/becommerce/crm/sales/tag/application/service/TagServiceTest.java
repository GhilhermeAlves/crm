package com.becommerce.crm.sales.tag.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.sales.tag.application.dto.CreateTagRequest;
import com.becommerce.crm.sales.tag.application.dto.TagResponse;
import com.becommerce.crm.sales.tag.application.port.out.TagRepository;
import com.becommerce.crm.sales.tag.domain.Tag;
import com.becommerce.crm.sales.tag.domain.exception.TagNotFoundException;
import com.becommerce.crm.sales.tag.domain.exception.TagValidationException;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TagServiceTest {

    private final TagRepository repository = mock(TagRepository.class);
    private final TagService service = new TagService(repository, mock(TenantAuditRecorder.class));
    private final UUID companyId = UUID.randomUUID();

    @Test
    void createsTagWithTrimmedName() {
        when(repository.findByCompanyIdAndName(companyId, "Ortodontia")).thenReturn(Optional.empty());

        TagResponse response = service.create(companyId, new CreateTagRequest("  Ortodontia ", "#22c55e"));

        assertEquals("Ortodontia", response.name());
        assertEquals(companyId, response.companyId());
        verify(repository).save(any(Tag.class));
    }

    @Test
    void rejectsDuplicatedName() {
        when(repository.findByCompanyIdAndName(companyId, "Ortodontia"))
                .thenReturn(Optional.of(Tag.create(companyId, "Ortodontia", null)));

        assertThrows(TagValidationException.class,
                () -> service.create(companyId, new CreateTagRequest("Ortodontia", null)));
        verify(repository, never()).save(any());
    }

    @Test
    void tagFromAnotherCompanyIsInvisible() {
        Tag foreign = Tag.create(UUID.randomUUID(), "Implante", null);
        when(repository.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        assertThrows(TagNotFoundException.class, () -> service.getById(companyId, foreign.getId()));
        assertThrows(TagNotFoundException.class, () -> service.delete(companyId, foreign.getId()));
        assertFalse(service.existsInCompany(companyId, foreign.getId()));
        verify(repository, never()).delete(any());
    }
}
