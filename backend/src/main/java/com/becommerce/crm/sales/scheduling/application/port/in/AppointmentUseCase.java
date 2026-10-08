package com.becommerce.crm.sales.scheduling.application.port.in;

import com.becommerce.crm.sales.scheduling.application.dto.*;
import com.becommerce.crm.sales.scheduling.domain.AppointmentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AppointmentUseCase {

    AppointmentResponse create(UUID companyId, CreateAppointmentRequest request, UUID createdBy);

    /** Como {@link #create}, registrando a origem (ex.: agendado pelo agente do WhatsApp). */
    AppointmentResponse create(UUID companyId, CreateAppointmentRequest request, UUID createdBy,
                               com.becommerce.crm.sales.scheduling.domain.AppointmentSource source);

    AppointmentResponse getById(UUID companyId, UUID appointmentId);

    AppointmentResponse update(UUID companyId, UUID appointmentId, UpdateAppointmentRequest request);

    AppointmentResponse reschedule(UUID companyId, UUID appointmentId, RescheduleRequest request);

    AppointmentResponse changeStatus(UUID companyId, UUID appointmentId, AppointmentStatus status);

    void delete(UUID companyId, UUID appointmentId);

    List<AppointmentResponse> list(UUID companyId, Instant from, Instant to, List<UUID> hostIds);
}
