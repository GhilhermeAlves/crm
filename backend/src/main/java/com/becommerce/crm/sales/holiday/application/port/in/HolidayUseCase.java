package com.becommerce.crm.sales.holiday.application.port.in;

import com.becommerce.crm.sales.holiday.application.dto.CreateHolidayRequest;
import com.becommerce.crm.sales.holiday.application.dto.HolidayResponse;
import com.becommerce.crm.sales.holiday.application.dto.UpdateHolidayRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface HolidayUseCase {

    HolidayResponse create(UUID companyId, CreateHolidayRequest request);

    HolidayResponse getById(UUID companyId, UUID holidayId);

    HolidayResponse update(UUID companyId, UUID holidayId, UpdateHolidayRequest request);

    void delete(UUID companyId, UUID holidayId);

    List<HolidayResponse> listByCompany(UUID companyId);

    List<HolidayResponse> listByRange(UUID companyId, LocalDate from, LocalDate to);
}
