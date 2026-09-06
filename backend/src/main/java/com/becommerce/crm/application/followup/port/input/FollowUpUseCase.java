package com.becommerce.crm.application.followup.port.input;

import com.becommerce.crm.application.followup.dto.FollowUpRequest;
import com.becommerce.crm.application.followup.dto.FollowUpResponse;
import com.becommerce.crm.application.identity.dto.PageResponse;

import java.util.UUID;

/**
 * Casos de uso de FollowUp (Sprint 22). Scoped à empresa do usuário autenticado
 * (o {@code companyId} vem do {@link com.becommerce.crm.infrastructure.security.filter.CurrentUser},
 * jamais do body) + RLS FORCE. Operações de leitura exigem a permissão
 * {@code omnichannel:followup:read}; escrita exige {@code omnichannel:followup}.
 */
public interface FollowUpUseCase {

    FollowUpResponse create(UUID companyId, FollowUpRequest request);

    FollowUpResponse get(UUID companyId, UUID followUpId);

    PageResponse<FollowUpResponse> list(UUID companyId, UUID conversationId, int page, int pageSize);

    FollowUpResponse cancel(UUID companyId, UUID followUpId);
}