package com.becommerce.crm.automation.ai.application.tool;

import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.domain.User;
import com.becommerce.crm.sales.scheduling.application.dto.AppointmentTypeResponse;
import com.becommerce.crm.sales.scheduling.application.port.in.AppointmentTypeUseCase;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Profissionais que podem ser responsaveis por agenda (Leo). Mesma regra do
 * agendamento pelo WhatsApp: os responsaveis dos tipos de consulta ativos; se a
 * clinica nao configurou responsaveis nos tipos, todos os usuarios ativos.
 */
@Component
public class SchedulingProfessionals {

    public record Professional(UUID id, String name) {
    }

    private final UserRepository userRepository;
    private final AppointmentTypeUseCase appointmentTypeUseCase;

    public SchedulingProfessionals(UserRepository userRepository, AppointmentTypeUseCase appointmentTypeUseCase) {
        this.userRepository = userRepository;
        this.appointmentTypeUseCase = appointmentTypeUseCase;
    }

    public List<Professional> list(UUID companyId) {
        Set<UUID> hostIds = new LinkedHashSet<>();
        for (AppointmentTypeResponse type : appointmentTypeUseCase.listByCompany(companyId)) {
            if (type.active() && type.hostIds() != null) {
                hostIds.addAll(type.hostIds());
            }
        }
        return userRepository.findAllByCompanyId(companyId).stream()
                .filter(User::isActive)
                .filter(u -> !u.isDeleted())
                .filter(u -> hostIds.isEmpty() || hostIds.contains(u.getId()))
                .map(u -> new Professional(u.getId(), displayName(u)))
                .sorted(Comparator.comparing(Professional::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static String displayName(User user) {
        String name = user.getName();
        if (name == null || name.isBlank()) {
            name = user.getFullName();
        }
        return name == null ? "" : name.trim();
    }
}
