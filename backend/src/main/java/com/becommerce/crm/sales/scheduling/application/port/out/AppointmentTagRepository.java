package com.becommerce.crm.sales.scheduling.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Marcadores (tags) vinculados aos agendamentos — tabela appointment_tags (V101). */
public interface AppointmentTagRepository {

    /** Marcadores de vários agendamentos numa única consulta (sem N+1). Ausentes = sem marcador. */
    Map<UUID, List<UUID>> findTagIds(Collection<UUID> appointmentIds);

    /** Substitui todos os marcadores do agendamento pelos informados. */
    void replace(UUID companyId, UUID appointmentId, Collection<UUID> tagIds);
}
