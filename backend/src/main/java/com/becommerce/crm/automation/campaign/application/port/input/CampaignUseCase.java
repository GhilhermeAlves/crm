package com.becommerce.crm.automation.campaign.application.port.input;

import com.becommerce.crm.automation.campaign.application.dto.CampaignResponse;
import com.becommerce.crm.automation.campaign.application.dto.CreateCampaignRequest;
import com.becommerce.crm.automation.campaign.application.dto.ExecutionResponse;
import com.becommerce.crm.shared.application.dto.PageResponse;
import com.becommerce.crm.automation.campaign.application.dto.ScheduleCampaignRequest;
import com.becommerce.crm.automation.campaign.application.dto.UpdateCampaignRequest;

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
                                   com.becommerce.crm.automation.campaign.application.dto.AttachChannelRequest request,
                                   UUID actorUserId);

    /** Execução imediata: DRAFT/SCHEDULED -> RUNNING e despacho assíncrono. */
    ExecutionResponse executeNow(UUID companyId, UUID campaignId, UUID actorUserId);

    CampaignResponse pause(UUID companyId, UUID campaignId, UUID actorUserId);

    CampaignResponse resume(UUID companyId, UUID campaignId, UUID actorUserId);

    CampaignResponse cancel(UUID companyId, UUID campaignId, UUID actorUserId);

    ExecutionResponse getExecution(UUID companyId, UUID campaignId);
}
