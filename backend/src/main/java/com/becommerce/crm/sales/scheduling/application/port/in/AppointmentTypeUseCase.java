package com.becommerce.crm.sales.scheduling.application.port.in;

import com.becommerce.crm.sales.scheduling.application.dto.AppointmentTypeResponse;
import com.becommerce.crm.sales.scheduling.application.dto.CreateAppointmentTypeRequest;
import com.becommerce.crm.sales.scheduling.application.dto.UpdateAppointmentTypeRequest;

import java.util.List;
import java.util.UUID;

public interface AppointmentTypeUseCase {

    AppointmentTypeResponse create(UUID companyId, CreateAppointmentTypeRequest request);

    AppointmentTypeResponse getById(UUID companyId, UUID typeId);

    AppointmentTypeResponse update(UUID companyId, UUID typeId, UpdateAppointmentTypeRequest request);

    void delete(UUID companyId, UUID typeId);

    List<AppointmentTypeResponse> listByCompany(UUID companyId);
}
