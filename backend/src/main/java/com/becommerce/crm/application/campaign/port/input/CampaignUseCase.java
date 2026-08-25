package com.becommerce.crm.application.campaign.port.input;

import com.becommerce.crm.application.campaign.dto.CampaignResponse;
import com.becommerce.crm.application.campaign.dto.CreateCampaignRequest;
import com.becommerce.crm.application.campaign.dto.ExecutionResponse;
import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.application.campaign.dto.ScheduleCampaignRequest;
import com.becommerce.crm.application.campaign.dto.UpdateCampaignRequest;

import java.util.UUID;

/** Casos de uso de Campanhas (Sprint 17): CRUD + ciclo de vida + execução. */
public interface CampaignUseCase {

    CampaignResponse create(UUID companyId, CreateCampaignRequest request, UUID createdBy);

    CampaignResponse getById(UUID companyId, UUID campaignId);

    CampaignResponse update(UUID companyId, UUID campaignId, UpdateCampaignRequest request);

    void delete(UUID companyId, UUID campaignId);

    PageResponse<CampaignResponse> list(UUID companyId, String status, String audienceType,
                                        int page, int pageSize);

    /** Define data de agendamento e move DRAFT -> SCHEDULED. */
    CampaignResponse schedule(UUID companyId, UUID campaignId, ScheduleCampaignRequest request,
                              UUID actorUserId);

    /** Vincula canal (Omnichannel) + template à campanha. */
    CampaignResponse attachChannel(UUID companyId, UUID campaignId,
                                   com.becommerce.crm.application.campaign.dto.AttachChannelRequest request,
                                   UUID actorUserId);

    /** Execução imediata: DRAFT/SCHEDULED -> RUNNING e despacho assíncrono. */
    ExecutionResponse executeNow(UUID companyId, UUID campaignId, UUID actorUserId);

    CampaignResponse pause(UUID companyId, UUID campaignId, UUID actorUserId);

    CampaignResponse resume(UUID companyId, UUID campaignId, UUID actorUserId);

    CampaignResponse cancel(UUID companyId, UUID campaignId, UUID actorUserId);

    ExecutionResponse getExecution(UUID companyId, UUID campaignId);
}
