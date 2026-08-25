package com.becommerce.crm.domain.campaign;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Canal da campanha (Sprint 17), tabela {@code campaign_channels} (V057).
 * Desacopla a campanha do provider: referencia um canal Omnichannel ativo
 * (V044) e congela a versão do template usada.
 */
public class CampaignChannel {

    private final UUID id;
    private final UUID companyId;
    private final UUID campaignId;
    private final String channelType;
    private final UUID providerChannelId;
    private final UUID templateId;
    private final int templateVersion;
    private final LocalDateTime createdAt;

    private CampaignChannel(UUID id, UUID companyId, UUID campaignId, String channelType,
                            UUID providerChannelId, UUID templateId, int templateVersion,
                            LocalDateTime createdAt) {
        this.id = id;
        this.companyId = companyId;
        this.campaignId = campaignId;
        this.channelType = channelType;
        this.providerChannelId = providerChannelId;
        this.templateId = templateId;
        this.templateVersion = templateVersion;
        this.createdAt = createdAt;
    }

    public static CampaignChannel create(UUID companyId, UUID campaignId, String channelType,
                                         UUID providerChannelId, UUID templateId, int templateVersion) {
        if (providerChannelId == null) {
            throw new IllegalArgumentException("Canal do provider é obrigatório.");
        }
        if (templateId == null) {
            throw new IllegalArgumentException("Template é obrigatório.");
        }
        return new CampaignChannel(UUID.randomUUID(), companyId, campaignId,
                channelType != null ? channelType : "WHATSAPP",
                providerChannelId, templateId, Math.max(1, templateVersion), LocalDateTime.now());
    }

    public static CampaignChannel reconstitute(UUID id, UUID companyId, UUID campaignId,
                                               String channelType, UUID providerChannelId,
                                               UUID templateId, int templateVersion,
                                               LocalDateTime createdAt) {
        return new CampaignChannel(id, companyId, campaignId, channelType, providerChannelId,
                templateId, templateVersion, createdAt);
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getCampaignId() { return campaignId; }
    public String getChannelType() { return channelType; }
    public UUID getProviderChannelId() { return providerChannelId; }
    public UUID getTemplateId() { return templateId; }
    public int getTemplateVersion() { return templateVersion; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
