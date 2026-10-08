package com.becommerce.crm.automation.ai.application.agent.context;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Contexto do paciente/contato para ESTA interação: só o necessário. Os
 * agendamentos vêm da agenda do CRM (fonte de verdade), não da memória.
 *
 * @param contactId   contato do CRM, se identificado (sem ele não há memória)
 * @param name        nome cadastrado no CRM
 * @param profileName nome do perfil no canal (ex.: WhatsApp)
 */
public record PatientContext(UUID contactId, String name, String profileName,
                             List<UpcomingAppointment> upcomingAppointments) {

    public PatientContext {
        upcomingAppointments = upcomingAppointments == null ? List.of() : List.copyOf(upcomingAppointments);
    }

    public static PatientContext unknown(String profileName) {
        return new PatientContext(null, null, profileName, List.of());
    }

    public boolean isIdentified() {
        return contactId != null;
    }

    public record UpcomingAppointment(String title, ZonedDateTime start, String status) {
    }
}
