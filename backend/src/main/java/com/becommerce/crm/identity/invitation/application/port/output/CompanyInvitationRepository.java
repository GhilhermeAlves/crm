package com.becommerce.crm.identity.invitation.application.port.output;

import com.becommerce.crm.identity.invitation.domain.CompanyInvitation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Convites de criação de empresa (V123). */
public interface CompanyInvitationRepository {

    CompanyInvitation save(CompanyInvitation invitation);

    Optional<CompanyInvitation> findById(UUID id);

    Optional<CompanyInvitation> findByTokenHash(String tokenHash);

    /** Com lock da linha: serializa aceites concorrentes do mesmo convite. */
    Optional<CompanyInvitation> findByTokenHashForUpdate(String tokenHash);

    /** Convites enviados a partir da empresa (RLS: inviter_company_id). */
    List<CompanyInvitation> findByInviterCompanyId(UUID inviterCompanyId);
}
