package com.becommerce.crm.sales.scheduling.infrastructure.persistence;

import com.becommerce.crm.sales.scheduling.domain.Appointment;
import com.becommerce.crm.sales.scheduling.domain.AppointmentSource;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Procedimentos e marcadores do agendamento são gravados por JDBC logo depois do save.
 * Se o INSERT do agendamento ficar pendente no Hibernate até o commit, a FK de
 * appointment_procedures/appointment_tags falha (erro 500 ao agendar com procedimento).
 */
class AppointmentRepositoryImplTest {

    @Test
    void saveFlushesSoJdbcChildRowsSeeTheAppointment() {
        AppointmentJpaRepository jpa = mock(AppointmentJpaRepository.class);
        when(jpa.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        AppointmentRepositoryImpl repository = new AppointmentRepositoryImpl(jpa);

        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        Appointment appointment = Appointment.create(UUID.randomUUID(), null, UUID.randomUUID(),
                UUID.randomUUID(), null, "Consulta", start, start.plus(1, ChronoUnit.HOURS),
                AppointmentSource.INTERNAL, null, null, null, UUID.randomUUID());

        repository.save(appointment);

        verify(jpa).saveAndFlush(any());
        verify(jpa, never()).save(any());
    }
}
