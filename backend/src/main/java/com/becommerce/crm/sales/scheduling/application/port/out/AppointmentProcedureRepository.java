package com.becommerce.crm.sales.scheduling.application.port.out;

import com.becommerce.crm.sales.scheduling.application.dto.ProcedureRef;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Procedimentos (serviços do catálogo) dos agendamentos — tabela appointment_procedures (V108). */
public interface AppointmentProcedureRepository {

    /** Procedimentos de vários agendamentos numa única consulta (sem N+1). Ausentes = sem procedimento. */
    Map<UUID, List<ProcedureRef>> findProcedures(Collection<UUID> appointmentIds);

    /** Substitui todos os procedimentos do agendamento pelos informados. */
    void replace(UUID companyId, UUID appointmentId, Collection<UUID> catalogItemIds);
}
