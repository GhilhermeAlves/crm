package com.becommerce.crm.automation.ai.application.tool;

import com.becommerce.crm.automation.ai.application.tool.SchedulingProfessionals.Professional;
import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.domain.User;
import com.becommerce.crm.identity.membership.application.port.output.MemberProjection;
import com.becommerce.crm.identity.membership.application.port.output.MembershipRepository;
import com.becommerce.crm.sales.scheduling.application.dto.AppointmentTypeResponse;
import com.becommerce.crm.sales.scheduling.application.port.in.AppointmentTypeUseCase;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SchedulingProfessionalsTest {

    private final UUID companyId = UUID.randomUUID();
    private final UserRepository users = mock(UserRepository.class);
    private final MembershipRepository memberships = mock(MembershipRepository.class);
    private final AppointmentTypeUseCase types = mock(AppointmentTypeUseCase.class);
    private final SchedulingProfessionals professionals = new SchedulingProfessionals(users, memberships, types);

    @BeforeEach
    void typesClearTenantLikeTheRealService() {
        when(types.listByCompany(companyId)).thenAnswer(inv -> {
            TenantContext.clear();
            return List.<AppointmentTypeResponse>of();
        });
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    /** Sem tenant a RLS esconde todos os membros: e o que o Telegram via (lista vazia). */
    @Test
    void readsMembersWithTenantEvenAfterTypeServiceClearsIt() {
        UUID memberId = UUID.randomUUID();
        MemberProjection member = member(memberId);
        when(memberships.findMembersByCompanyIdAndStatus(companyId, "ACTIVE")).thenAnswer(inv ->
                companyId.equals(TenantContext.getCompanyId()) ? List.of(member) : List.of());
        User user = user(memberId, "Dra. Ana", true, false);
        when(users.findById(memberId)).thenAnswer(inv ->
                companyId.equals(TenantContext.getCompanyId()) ? Optional.of(user) : Optional.empty());

        List<Professional> result = professionals.list(companyId);

        assertEquals(List.of(new Professional(memberId, "Dra. Ana")), result);
    }

    @Test
    void skipsInactiveAndDeletedMembers() {
        UUID active = UUID.randomUUID();
        UUID inactive = UUID.randomUUID();
        UUID deleted = UUID.randomUUID();
        List<MemberProjection> members = List.of(member(active), member(inactive), member(deleted));
        when(memberships.findMembersByCompanyIdAndStatus(companyId, "ACTIVE")).thenReturn(members);
        User activeUser = user(active, "Ana", true, false);
        User inactiveUser = user(inactive, "Bia", false, false);
        User deletedUser = user(deleted, "Caio", true, true);
        when(users.findById(active)).thenReturn(Optional.of(activeUser));
        when(users.findById(inactive)).thenReturn(Optional.of(inactiveUser));
        when(users.findById(deleted)).thenReturn(Optional.of(deletedUser));

        assertEquals(List.of(new Professional(active, "Ana")), professionals.list(companyId));
    }

    @Test
    void restoresPreviousTenantState() {
        when(memberships.findMembersByCompanyIdAndStatus(companyId, "ACTIVE")).thenReturn(List.of());

        professionals.list(companyId);

        assertNull(TenantContext.getCompanyId());
    }

    private static MemberProjection member(UUID id) {
        MemberProjection member = mock(MemberProjection.class);
        when(member.getUserId()).thenReturn(id);
        return member;
    }

    private static User user(UUID id, String name, boolean active, boolean deleted) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getName()).thenReturn(name);
        when(user.isActive()).thenReturn(active);
        when(user.isDeleted()).thenReturn(deleted);
        return user;
    }
}
