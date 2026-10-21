package com.becommerce.crm.sales.scheduling.application.service;

import com.becommerce.crm.sales.scheduling.application.dto.AgendaSettingsDto;
import com.becommerce.crm.sales.scheduling.infrastructure.persistence.AgendaSettingsJpaEntity;
import com.becommerce.crm.sales.scheduling.infrastructure.persistence.AgendaSettingsJpaRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendaSettingsServiceTest {

    private final UUID companyId = UUID.randomUUID();

    @Mock AgendaSettingsJpaRepository repository;

    @InjectMocks AgendaSettingsService service;

    @Test
    void get_withoutSavedSettings_returnsDefaults() {
        when(repository.findById(companyId)).thenReturn(Optional.empty());

        AgendaSettingsDto result = service.get(companyId);

        assertEquals(AgendaSettingsDto.defaults(), result);
        assertEquals(30, result.defaultDurationMinutes());
        assertFalse(result.notify1hBefore());
    }

    @Test
    void save_createsRowForCompany() {
        when(repository.findById(companyId)).thenReturn(Optional.empty());
        AgendaSettingsDto request = new AgendaSettingsDto(45, LocalTime.of(7, 30), LocalTime.of(19, 0),
                false, true, true, true, false);

        AgendaSettingsDto result = service.save(companyId, request);

        ArgumentCaptor<AgendaSettingsJpaEntity> saved = ArgumentCaptor.forClass(AgendaSettingsJpaEntity.class);
        verify(repository).save(saved.capture());
        assertEquals(companyId, saved.getValue().getCompanyId());
        assertEquals(45, saved.getValue().getDefaultDurationMinutes());
        assertNotNull(saved.getValue().getUpdatedAt());
        assertEquals(request, result);
    }

    @Test
    void validation_rejectsInvertedWorkHoursAndOutOfRangeDuration() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        AgendaSettingsDto inverted = new AgendaSettingsDto(30, LocalTime.of(18, 0), LocalTime.of(8, 0),
                true, true, false, true, true);
        AgendaSettingsDto tooShort = new AgendaSettingsDto(2, LocalTime.of(8, 0), LocalTime.of(18, 0),
                true, true, false, true, true);

        assertFalse(validator.validate(inverted).isEmpty());
        assertFalse(validator.validate(tooShort).isEmpty());
        assertTrue(validator.validate(AgendaSettingsDto.defaults()).isEmpty());
    }
}
