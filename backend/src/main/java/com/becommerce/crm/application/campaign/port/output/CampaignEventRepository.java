package com.becommerce.crm.application.campaign.port.output;

import com.becommerce.crm.domain.campaign.CampaignChannel;
import com.becommerce.crm.domain.campaign.CampaignMessageEvent;
import com.becommerce.crm.domain.campaign.MessageEventStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída para eventos por destinatário e canal da campanha
 * ({@code campaign_message_events} / {@code campaign_channels}, V057/V059).
 * A UNIQUE (execution_id, recipient_id) no banco garante idempotência.
 */
public interface CampaignEventRepository {

    /** Insere eventos ignorando duplicados (ON CONFLICT DO NOTHING). Retorna inseridos. */
    int insertAllIgnoringConflicts(List<CampaignMessageEvent> events);

    void saveEvent(CampaignMessageEvent event);

    Optional<CampaignMessageEvent> findById(UUID id);

    /** Próximo lote PENDING de uma execução, ordenado deterministicamente. */
    List<CampaignMessageEvent> findPendingBatch(UUID executionId, int limit);

    long countByExecutionAndStatus(UUID executionId, MessageEventStatus status);

    /** Eventos já resolvidos (não-PENDING) de uma execução. */
    long countNotPendingByExecution(UUID executionId);

    /** Cancela todos os PENDING da execução. Retorna quantidade cancelada. */
    int cancelPendingByExecution(UUID executionId);

    PageResult findByCampaignWithFilters(UUID companyId, UUID campaignId, String status,
                                         int page, int pageSize);

    Optional<CampaignChannel> findChannelByCampaignId(UUID campaignId);

    CampaignChannel saveChannel(CampaignChannel channel);

    record PageResult(List<CampaignMessageEvent> content, long totalElements) {}
}
