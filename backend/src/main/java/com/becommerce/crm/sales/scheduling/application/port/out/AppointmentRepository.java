package com.becommerce.crm.sales.scheduling.application.port.out;

import com.becommerce.crm.sales.scheduling.domain.Appointment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository {

    Appointment save(Appointment appointment);

    Optional<Appointment> findById(UUID id);

    List<Appointment> findByCompanyIdAndRange(UUID companyId, Instant from, Instant to, List<UUID> hostIds);

    List<Appointment> findNonCanceledByHostAndRange(UUID hostId, Instant from, Instant to);

    Optional<Appointment> findByPublicToken(String publicToken);

    /** Próximos agendamentos não cancelados do contato (contexto do paciente para o agente de IA). */
    List<Appointment> findUpcomingByContact(UUID companyId, UUID contactId, Instant from, int limit);

    long countByHostIdInWeek(UUID hostId, Instant weekStart, Instant weekEnd);

    void delete(Appointment appointment);
}
