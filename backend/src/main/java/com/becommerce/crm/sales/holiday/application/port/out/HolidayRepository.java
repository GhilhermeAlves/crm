package com.becommerce.crm.sales.holiday.application.port.out;

import com.becommerce.crm.sales.holiday.domain.CompanyHoliday;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HolidayRepository {

    CompanyHoliday save(CompanyHoliday holiday);

    Optional<CompanyHoliday> findById(UUID id);

    List<CompanyHoliday> findByCompanyId(UUID companyId);

    List<CompanyHoliday> findByCompanyIdAndDateRange(UUID companyId, LocalDate from, LocalDate to);

    Optional<CompanyHoliday> findByCompanyIdAndDate(UUID companyId, LocalDate date);

    void delete(CompanyHoliday holiday);
}
