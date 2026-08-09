package com.becommerce.crm.application.membership.port.input;

import com.becommerce.crm.application.membership.dto.MemberResponse;
import com.becommerce.crm.application.membership.dto.MembershipResponse;

import java.util.List;
import java.util.UUID;

public interface MembershipUseCase {

    List<MemberResponse> listMembers(UUID companyId, UUID requesterCompanyId);

    List<MembershipResponse> listMyMemberships(UUID userId);

    MemberResponse updateMemberRole(UUID companyId, UUID userId, String role,
                                    UUID requesterCompanyId, boolean isSuperAdmin);

    void removeMember(UUID companyId, UUID userId,
                      UUID requesterCompanyId, boolean isSuperAdmin);
}
