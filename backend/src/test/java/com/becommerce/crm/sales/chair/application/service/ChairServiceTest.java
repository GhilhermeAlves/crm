package com.becommerce.crm.sales.chair.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.sales.chair.application.dto.ChairResponse;
import com.becommerce.crm.sales.chair.application.dto.CreateChairRequest;
import com.becommerce.crm.sales.chair.application.port.out.ChairRepository;
import com.becommerce.crm.sales.chair.domain.Chair;
import com.becommerce.crm.sales.chair.domain.exception.ChairNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChairServiceTest {

    private final ChairRepository repository = mock(ChairRepository.class);
    private final ChairService service = new ChairService(repository, mock(TenantAuditRecorder.class));
    private final UUID companyId = UUID.randomUUID();

    @Test
    void createsChairForCompany() {
        when(repository.findByCompanyIdAndName(any(), any())).thenReturn(Optional.empty());

        ChairResponse response = service.create(companyId, new CreateChairRequest("Cadeira 1", "#0ea5e9", true, 1));

        assertEquals("Cadeira 1", response.name());
        assertEquals(companyId, response.companyId());
        verify(repository).save(any(Chair.class));
    }

    @Test
    void listsOnlyCompanyChairs() {
        Chair own = Chair.create(companyId, "Cadeira 1", null, true, 0);
        when(repository.findByCompanyId(companyId)).thenReturn(List.of(own));

        assertEquals(List.of(own.getId()), service.listByCompany(companyId).stream().map(ChairResponse::id).toList());
    }

    @Test
    void chairFromAnotherCompanyIsInvisible() {
        Chair foreign = Chair.create(UUID.randomUUID(), "Cadeira X", null, true, 0);
        when(repository.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        assertThrows(ChairNotFoundException.class, () -> service.getById(companyId, foreign.getId()));
        assertThrows(ChairNotFoundException.class, () -> service.delete(companyId, foreign.getId()));
        assertFalse(service.existsInCompany(companyId, foreign.getId()));
        verify(repository, never()).delete(any());
    }
}
