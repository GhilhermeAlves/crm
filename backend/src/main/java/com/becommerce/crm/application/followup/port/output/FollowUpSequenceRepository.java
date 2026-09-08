package com.becommerce.crm.application.followup.port.output;

import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.domain.followup.FollowUpSequence;

import java.util.Optional;
import java.util.UUID;

/**
 * Porta de persistência de {@link FollowUpSequence} (Sprint 22). Scoped à
 * empresa (companyId sempre presente; o banco ainda aplica RLS FORCE).
 */
public interface FollowUpSequenceRepository {

    FollowUpSequence save(FollowUpSequence sequence);

    Optional<FollowUpSequence> findById(UUID id);

    PageResponse<FollowUpSequence> findByCompany(UUID companyId, int page, int pageSize);

    /** Exclusão física (ON DELETE SET NULL em follow-ups referenciais). */
    void delete(UUID companyId, UUID sequenceId);
}