package com.becommerce.crm.automation.ai.application.agent.context;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
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

    /**
     * Primeiro nome para tratar o paciente: o do cadastro do CRM tem prioridade;
     * sem ele, o do perfil do canal, só quando parece nome de pessoa (letras,
     * sem números/emojis — perfis como "Loja X 2024" ou "🦷" ficam de fora).
     */
    public Optional<String> firstName() {
        return firstWord(name).or(() -> firstWord(profileName));
    }

    private static Optional<String> firstWord(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return Optional.empty();
        }
        String first = fullName.trim().split("\\s+")[0];
        if (!first.matches("\\p{L}[\\p{L}'-]{1,29}")) {
            return Optional.empty();
        }
        return Optional.of(first.substring(0, 1).toUpperCase(java.util.Locale.ROOT)
                + first.substring(1).toLowerCase(java.util.Locale.ROOT));
    }

    public record UpcomingAppointment(String title, ZonedDateTime start, String status) {
    }
}
