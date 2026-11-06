package com.becommerce.crm.automation.ai.application.tool;

import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.domain.User;
import com.becommerce.crm.identity.membership.application.port.output.MemberProjection;
import com.becommerce.crm.identity.membership.application.port.output.MembershipRepository;
import com.becommerce.crm.sales.scheduling.application.dto.AppointmentTypeResponse;
import com.becommerce.crm.sales.scheduling.application.port.in.AppointmentTypeUseCase;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
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
    private final MembershipRepository membershipRepository;
    private final AppointmentTypeUseCase appointmentTypeUseCase;

    public SchedulingProfessionals(UserRepository userRepository, MembershipRepository membershipRepository,
                                   AppointmentTypeUseCase appointmentTypeUseCase) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.appointmentTypeUseCase = appointmentTypeUseCase;
    }

    public List<Professional> list(UUID companyId) {
        UUID previous = TenantContext.getCompanyId();
        try {
            Set<UUID> hostIds = new LinkedHashSet<>();
            for (AppointmentTypeResponse type : appointmentTypeUseCase.listByCompany(companyId)) {
                if (type.active() && type.hostIds() != null) {
                    hostIds.addAll(type.hostIds());
                }
            }
            // listByCompany limpa o TenantContext no finally; sem tenant a RLS de
            // users/memberships esconde todos (no Telegram nao ha keycloak_sub de reserva).
            TenantContext.setCompanyId(companyId);
            // Parte de memberships, nao de users.company_id: quem atua em varias
            // clinicas tem company_id apontando para a empresa ativa, nao esta.
            return membershipRepository.findMembersByCompanyIdAndStatus(companyId, "ACTIVE").stream()
                    .map(MemberProjection::getUserId)
                    .filter(id -> hostIds.isEmpty() || hostIds.contains(id))
                    .map(userRepository::findById)
                    .flatMap(Optional::stream)
                    .filter(User::isActive)
                    .filter(u -> !u.isDeleted())
                    .map(u -> new Professional(u.getId(), displayName(u)))
                    .sorted(Comparator.comparing(Professional::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } finally {
            if (previous == null) {
                TenantContext.clear();
            } else {
                TenantContext.setCompanyId(previous);
            }
        }
    }

    private static String displayName(User user) {
        String name = user.getName();
        if (name == null || name.isBlank()) {
            name = user.getFullName();
        }
        return name == null ? "" : name.trim();
    }
}
