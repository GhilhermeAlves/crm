package com.becommerce.crm.application.followup.port.input;

import com.becommerce.crm.application.followup.dto.FollowUpSequenceRequest;
import com.becommerce.crm.application.followup.dto.FollowUpSequenceResponse;
import com.becommerce.crm.application.identity.dto.PageResponse;

import java.util.UUID;

/**
 * Casos de uso de {@link FollowUpSequence} (Sprint 22). Scoped à empresa do
 * usuário autenticado (companyId vem do CurrentUser, nunca do body) + RLS
 * FORCE. Armazenamento restrito a ADMIN/MANAGER via permissões
 * {@code omnichannel:followup:sequence} (manage) e
 * {@code omnichannel:followup:sequence:read} (read).
 */
public interface FollowUpSequenceUseCase {

    FollowUpSequenceResponse create(UUID companyId, FollowUpSequenceRequest request);

    FollowUpSequenceResponse get(UUID companyId, UUID sequenceId);

    PageResponse<FollowUpSequenceResponse> list(UUID companyId, int page, int pageSize);

    FollowUpSequenceResponse update(UUID companyId, UUID sequenceId, FollowUpSequenceRequest request);

    void delete(UUID companyId, UUID sequenceId);

    FollowUpSequenceResponse activate(UUID companyId, UUID sequenceId);

    FollowUpSequenceResponse deactivate(UUID companyId, UUID sequenceId);
}