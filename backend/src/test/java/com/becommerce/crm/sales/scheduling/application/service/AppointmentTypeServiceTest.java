package com.becommerce.crm.sales.scheduling.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.sales.scheduling.application.dto.UpdateAppointmentTypeRequest;
import com.becommerce.crm.sales.scheduling.application.port.out.AppointmentTypeRepository;
import com.becommerce.crm.sales.scheduling.domain.AppointmentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentTypeServiceTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID hostId = UUID.randomUUID();

    @Mock AppointmentTypeRepository repository;
    @Mock TenantAuditRecorder auditor;

    @InjectMocks AppointmentTypeService service;

    private AppointmentType activeType() {
        AppointmentType type = AppointmentType.create(companyId, "Avaliação", "avaliacao", null,
                60, 0, 0, 1, 60, 30, null, null, null, null, List.of(hostId));
        when(repository.findById(type.getId())).thenReturn(Optional.of(type));
        when(repository.findByCompanyIdAndSlug(companyId, "avaliacao")).thenReturn(Optional.of(type));
        return type;
    }

    private UpdateAppointmentTypeRequest request(Boolean active) {
        return new UpdateAppointmentTypeRequest("Avaliação", "avaliacao", null, 60, 0, 0, 1, 60, 30,
                null, null, null, null, false, active, List.of(hostId));
    }

    @Test
    void update_withoutActiveField_keepsTypeActive() {
        AppointmentType type = activeType();
        assertTrue(type.isActive());
        service.update(companyId, type.getId(), request(null));
        assertTrue(type.isActive());
    }

    @Test
    void update_withActiveFalse_deactivates() {
        AppointmentType type = activeType();
        service.update(companyId, type.getId(), request(false));
        assertFalse(type.isActive());
    }
}
