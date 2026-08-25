package com.becommerce.crm.application.campaign.port.output;

import com.becommerce.crm.domain.campaign.Campaign;
import com.becommerce.crm.domain.campaign.MessageEventStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída para campanhas ({@code campaigns}, V056).
 * RLS FORCE isola o tenant; os métodos de claim usam UPDATE condicional para
 * garantir transição atômica sob concorrência (idempotência do START).
 */
public interface CampaignRepository {

    Campaign save(Campaign campaign);

    Optional<Campaign> findById(UUID id);

    void delete(Campaign campaign);

    PageResult findByCompanyWithFilters(UUID companyId, String status, String audienceType,
                                        int page, int pageSize);

    /**
     * Claim atômico: promove SCHEDULED -> RUNNING apenas se ainda estiver em
     * SCHEDULED. Retorna false quando outra thread/instância já iniciou.
     */
    boolean claimForExecution(UUID campaignId);

    /** Devolve a campanha para SCHEDULED (usado quando o público está vazio no start). */
    boolean resetToScheduled(UUID campaignId);

    /** Conclui a campanha atômicamente se estiver RUNNING. */
    boolean completeIfRunning(UUID campaignId);

    /** Campanhas agendadas vencidas, para o scheduler (limite aplicado). */
    List<Campaign> findDueForExecution(LocalDateTime now, int limit);

    record PageResult(List<Campaign> content, long totalElements) {}
}
