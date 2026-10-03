package com.becommerce.crm.sales.scheduling.application.port.out;

import com.becommerce.crm.sales.scheduling.domain.AppointmentType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentTypeRepository {

    AppointmentType save(AppointmentType type);

    Optional<AppointmentType> findById(UUID id);

    Optional<AppointmentType> findByCompanyIdAndSlug(UUID companyId, String slug);

    List<AppointmentType> findByCompanyId(UUID companyId);

    void delete(AppointmentType type);
}
