package com.becommerce.crm.automation.campaign.application.port.output;

import com.becommerce.crm.automation.campaign.domain.AudienceType;

import com.becommerce.crm.automation.campaign.domain.CampaignMessageRenderer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolução do público da campanha (PLAN.md seção 5). Estratégia por
 * {@link AudienceType}; critérios em JSON. Deve ser determinística (ORDER BY id)
 * para execução idempotente e respeitar o tenant.
 */
public interface AudienceResolver {

    List<Recipient> resolve(UUID companyId, AudienceType audienceType, String audienceCriteriaJson);

    /** Dados do contato para personalizar a mensagem; vazio se o contato não existir mais. */
    Optional<CampaignMessageRenderer.Profile> findProfile(UUID companyId, UUID contactId);

    record Recipient(UUID id, String type, String phone) {}
}
