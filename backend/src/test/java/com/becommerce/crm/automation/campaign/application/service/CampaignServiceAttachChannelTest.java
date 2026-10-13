package com.becommerce.crm.automation.campaign.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.automation.campaign.application.dto.AttachChannelRequest;
import com.becommerce.crm.automation.campaign.application.port.output.AudienceResolver;
import com.becommerce.crm.automation.campaign.application.port.output.CampaignEventRepository;
import com.becommerce.crm.automation.campaign.application.port.output.CampaignRepository;
import com.becommerce.crm.automation.campaign.domain.AudienceType;
import com.becommerce.crm.automation.campaign.domain.Campaign;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelChannelRepository;
import com.becommerce.crm.communication.omnichannel.domain.Channel;
import com.becommerce.crm.communication.omnichannel.domain.ChannelProvider;
import com.becommerce.crm.communication.omnichannel.domain.ChannelStatus;
import com.becommerce.crm.communication.omnichannel.domain.ChannelType;
import com.becommerce.crm.communication.template.application.port.output.TemplateRepository;
import com.becommerce.crm.communication.template.domain.MessageTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignServiceAttachChannelTest {

    @Mock CampaignRepository campaignRepository;
    @Mock CampaignEventRepository eventRepository;
    @Mock AudienceResolver audienceResolver;
    @Mock CampaignExecutionService executionService;
    @Mock TenantAuditRecorder auditor;
    @Mock OmnichannelChannelRepository channelRepository;
    @Mock TemplateRepository templateRepository;

    private CampaignService service;
    private final UUID companyId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CampaignService(campaignRepository, eventRepository, audienceResolver,
                executionService, auditor, channelRepository, templateRepository);
    }

    private Campaign draftCampaign() {
        Campaign campaign = Campaign.create(companyId, "Black Friday", null, AudienceType.CONTACTS, null, null, userId);
        when(campaignRepository.findById(campaign.getId())).thenReturn(Optional.of(campaign));
        return campaign;
    }

    private Channel channel(ChannelStatus status) {
        Channel channel = Channel.reconstitute(UUID.randomUUID(), companyId, ChannelType.WHATSAPP,
                ChannelProvider.EVOLUTION, "WhatsApp", status, "5534999999999", null, null,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
        when(channelRepository.findById(channel.getId())).thenReturn(Optional.of(channel));
        return channel;
    }

    @Test
    void attachesActiveChannel() {
        Campaign campaign = draftCampaign();
        Channel channel = channel(ChannelStatus.ACTIVE);
        MessageTemplate template = MessageTemplate.create(companyId, "Oferta", "WHATSAPP", null, "Olá {{nome}}", null, null);
        when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));

        service.attachChannel(companyId, campaign.getId(),
                new AttachChannelRequest("WHATSAPP", channel.getId(), template.getId()), userId);

        verify(eventRepository).saveChannel(any());
    }

    @Test
    void rejectsInactiveChannel() {
        Campaign campaign = draftCampaign();
        Channel channel = channel(ChannelStatus.INACTIVE);

        assertThatThrownBy(() -> service.attachChannel(companyId, campaign.getId(),
                new AttachChannelRequest("WHATSAPP", channel.getId(), UUID.randomUUID()), userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ACTIVE");
        verify(eventRepository, never()).saveChannel(any());
    }
}
