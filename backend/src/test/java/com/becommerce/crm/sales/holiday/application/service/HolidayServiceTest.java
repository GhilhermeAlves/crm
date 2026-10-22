package com.becommerce.crm.sales.holiday.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.sales.holiday.application.dto.CreateHolidayRequest;
import com.becommerce.crm.sales.holiday.application.dto.HolidayResponse;
import com.becommerce.crm.sales.holiday.application.port.out.HolidayRepository;
import com.becommerce.crm.sales.holiday.domain.CompanyHoliday;
import com.becommerce.crm.sales.holiday.domain.exception.HolidayNotFoundException;
import com.becommerce.crm.sales.holiday.domain.exception.HolidayValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HolidayServiceTest {

    private final HolidayRepository repository = mock(HolidayRepository.class);
    private final HolidayService service = new HolidayService(repository, mock(TenantAuditRecorder.class));
    private final UUID companyId = UUID.randomUUID();
    private final LocalDate date = LocalDate.of(2026, 11, 20);

    @Test
    void createsBlockingHolidayByDefault() {
        when(repository.findByCompanyIdAndDate(companyId, date)).thenReturn(Optional.empty());

        HolidayResponse response = service.create(companyId,
                new CreateHolidayRequest(date, "Recesso", null, null));

        assertTrue(response.blocksSchedule());
        assertFalse(response.recurringYearly());
        verify(repository).save(any(CompanyHoliday.class));
    }

    @Test
    void rejectsSecondHolidayOnSameDate() {
        when(repository.findByCompanyIdAndDate(companyId, date))
                .thenReturn(Optional.of(CompanyHoliday.create(companyId, date, "Recesso", false, true)));

        assertThrows(HolidayValidationException.class,
                () -> service.create(companyId, new CreateHolidayRequest(date, "Outro", false, true)));
    }

    @Test
    void holidayFromAnotherCompanyIsInvisible() {
        CompanyHoliday foreign = CompanyHoliday.create(UUID.randomUUID(), date, "Recesso", false, true);
        when(repository.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        assertThrows(HolidayNotFoundException.class, () -> service.delete(companyId, foreign.getId()));
        verify(repository, never()).delete(any());
    }
}
