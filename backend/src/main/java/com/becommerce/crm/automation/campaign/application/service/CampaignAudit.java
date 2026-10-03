package com.becommerce.crm.automation.campaign.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;

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
