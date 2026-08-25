package com.becommerce.crm.application.campaign.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.campaign.dto.CreateCampaignRequest;
import com.becommerce.crm.application.campaign.dto.CampaignResponse;
import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.audit.AuditModule;

import java.util.Map;
import java.util.UUID;

/** Suporte compartilhado aos serviços de campanha (auditoria no módulo CAMPAIGNS). */
final class CampaignAudit {

    private CampaignAudit() {
    }

    static void record(TenantAuditRecorder auditor, UUID companyId, AuditAction action,
                       UUID campaignId, String description, UUID actorUserId,
                       Map<String, Object> details) {
        auditor.record(companyId, action, AuditModule.CAMPAIGNS, "Campaign",
                campaignId.toString(), description, actorUserId, details);
    }
}
