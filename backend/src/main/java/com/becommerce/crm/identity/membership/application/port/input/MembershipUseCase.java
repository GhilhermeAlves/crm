package com.becommerce.crm.identity.membership.application.port.input;

import com.becommerce.crm.identity.membership.application.dto.MemberResponse;
import com.becommerce.crm.identity.membership.application.dto.MembershipResponse;
import com.becommerce.crm.identity.membership.domain.MembershipStatus;

import java.util.List;
import java.util.UUID;

public interface MembershipUseCase {

    /**
     * Membros da empresa. {@code status == null} mantém o comportamento histórico
     * de listar apenas {@code ACTIVE}; {@link MembershipStatus#REMOVED} é a fonte
     * de dados dos desligados (aba Inativos).
     */
    List<MemberResponse> listMembers(UUID companyId, UUID requesterCompanyId, MembershipStatus status);

    List<MembershipResponse> listMyMemberships(UUID userId);

    MemberResponse updateMemberRole(UUID companyId, UUID userId, String role,
                                    UUID requesterCompanyId, boolean isSuperAdmin);

    void removeMember(UUID companyId, UUID userId,
                      UUID requesterCompanyId, boolean isSuperAdmin);
}
