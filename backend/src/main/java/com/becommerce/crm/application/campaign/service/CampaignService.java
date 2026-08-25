package com.becommerce.crm.application.campaign.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.campaign.dto.CreateCampaignRequest;
import com.becommerce.crm.application.campaign.dto.CampaignResponse;
import com.becommerce.crm.application.campaign.dto.ScheduleCampaignRequest;
import com.becommerce.crm.application.campaign.dto.UpdateCampaignRequest;
import com.becommerce.crm.application.campaign.port.input.CampaignUseCase;
import com.becommerce.crm.application.campaign.port.output.AudienceResolver;
import com.becommerce.crm.application.campaign.port.output.CampaignEventRepository;
import com.becommerce.crm.application.campaign.port.output.CampaignRepository;
import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.campaign.AudienceType;
import com.becommerce.crm.domain.campaign.Campaign;
import com.becommerce.crm.domain.campaign.CampaignStatus;
import com.becommerce.crm.domain.campaign.MessageEventStatus;
import com.becommerce.crm.domain.campaign.exception.CampaignNotFoundException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Campanhas (Sprint 17). Isolamento por {@code TenantContext} + RLS FORCE
 * (V056); transições de ciclo de vida controladas no domínio; auditoria via
 * {@link TenantAuditRecorder} reutilizando a infraestrutura existente.
 */
@Service
public class CampaignService implements CampaignUseCase {

    private final CampaignRepository campaignRepository;
    private final CampaignEventRepository eventRepository;
    private final AudienceResolver audienceResolver;
    private final CampaignExecutionService executionService;
    private final TenantAuditRecorder auditor;
    private final com.becommerce.crm.application.omnichannel.port.output.OmnichannelChannelRepository omnichannelChannelRepository;
    private final com.becommerce.crm.application.template.port.output.TemplateRepository templateRepository;

    public CampaignService(CampaignRepository campaignRepository,
                           CampaignEventRepository eventRepository,
                           AudienceResolver audienceResolver,
                           CampaignExecutionService executionService,
                           TenantAuditRecorder auditor,
                           com.becommerce.crm.application.omnichannel.port.output.OmnichannelChannelRepository omnichannelChannelRepository,
                           com.becommerce.crm.application.template.port.output.TemplateRepository templateRepository) {
        this.campaignRepository = campaignRepository;
        this.eventRepository = eventRepository;
        this.audienceResolver = audienceResolver;
        this.executionService = executionService;
        this.auditor = auditor;
        this.omnichannelChannelRepository = omnichannelChannelRepository;
        this.templateRepository = templateRepository;
    }

    @Override
    @Transactional
    public CampaignResponse create(UUID companyId, CreateCampaignRequest request, UUID createdBy) {
        try {
            TenantContext.setCompanyId(companyId);
            AudienceType audienceType = AudienceType.valueOf(request.audienceType());
            int estimated = audienceResolver.resolve(companyId, audienceType,
                    request.audienceCriteria()).size();

            Campaign campaign = Campaign.create(companyId, request.name(), request.description(),
                    audienceType, request.audienceCriteria(), request.timezone(), createdBy);
            campaign.setEstimatedRecipients(estimated);
            Campaign saved = campaignRepository.save(campaign);

            CampaignAudit.record(auditor, companyId, AuditAction.CREATE, saved.getId(),
                    "Campanha criada: " + saved.getName(), createdBy,
                    Map.of("audienceType", saved.getAudienceType().name(),
                            "estimatedRecipients", String.valueOf(estimated)));
            return withChannel(saved);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignResponse getById(UUID companyId, UUID campaignId) {
        try {
            TenantContext.setCompanyId(companyId);
            return withChannel(requireOwned(companyId, campaignId));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public CampaignResponse update(UUID companyId, UUID campaignId, UpdateCampaignRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            Campaign campaign = requireOwned(companyId, campaignId);
            campaign.updateDetails(request.name(), request.description());
            Campaign saved = campaignRepository.save(campaign);

            CampaignAudit.record(auditor, companyId, AuditAction.UPDATE, saved.getId(),
                    "Campanha atualizada", null, Map.of("status", saved.getStatus().name()));
            return withChannel(saved);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID campaignId) {
        try {
            TenantContext.setCompanyId(companyId);
            Campaign campaign = requireOwned(companyId, campaignId);
            if (campaign.getStatus() == CampaignStatus.RUNNING) {
                throw new IllegalStateException("Campanha em execução não pode ser excluída; cancele-a primeiro.");
            }
            campaignRepository.delete(campaign);

            CampaignAudit.record(auditor, companyId, AuditAction.DELETE, campaignId,
                    "Campanha excluída", null, Map.of("name", campaign.getName()));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CampaignResponse> list(UUID companyId, String status, String audienceType,
                                               int page, int pageSize) {
        try {
            TenantContext.setCompanyId(companyId);
            var result = campaignRepository.findByCompanyWithFilters(companyId,
                    normalizeEnum(status), normalizeEnum(audienceType), page, pageSize);
            List<CampaignResponse> content = result.content().stream()
                    .map(this::withChannel).toList();
            return PageResponse.of(content, page, pageSize, result.totalElements());
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public CampaignResponse attachChannel(UUID companyId, UUID campaignId,
                                          com.becommerce.crm.application.campaign.dto.AttachChannelRequest request,
                                          UUID actorUserId) {
        try {
            TenantContext.setCompanyId(companyId);
            Campaign campaign = requireOwned(companyId, campaignId);
            if (campaign.getStatus() != CampaignStatus.DRAFT && campaign.getStatus() != CampaignStatus.SCHEDULED) {
                throw new IllegalStateException("Canal só pode ser alterado em DRAFT ou SCHEDULED.");
            }
            // valida posse do canal omnichannel e do template
            var omniChannel = omnichannelChannelRepository.findById(request.providerChannelId())
                    .orElseThrow(() -> new IllegalArgumentException("Canal não encontrado."));
            if (!omniChannel.getCompanyId().equals(companyId)) {
                throw new IllegalArgumentException("Canal não encontrado.");
            }
            if (!"ACTIVE".equals(omniChannel.getStatus())) {
                throw new IllegalStateException("Canal precisa estar ACTIVE.");
            }
            com.becommerce.crm.domain.template.MessageTemplate template =
                    templateRepository.findById(request.templateId())
                            .orElseThrow(() -> new com.becommerce.crm.domain.template.exception.TemplateNotFoundException(
                                    request.templateId()));
            if (!template.getCompanyId().equals(companyId)) {
                throw new com.becommerce.crm.domain.template.exception.TemplateNotFoundException(
                        request.templateId());
            }

            String channelType = request.channelType() != null ? request.channelType()
                    : omniChannel.getType().name();
            eventRepository.saveChannel(com.becommerce.crm.domain.campaign.CampaignChannel.create(
                    companyId, campaignId, channelType, request.providerChannelId(),
                    request.templateId(), template.getVersion()));

            CampaignAudit.record(auditor, companyId, AuditAction.UPDATE, campaignId,
                    "Canal/template vinculado à campanha", actorUserId,
                    Map.of("channelType", channelType,
                            "templateId", request.templateId().toString()));
            return withChannel(campaign);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public CampaignResponse schedule(UUID companyId, UUID campaignId,
                                     ScheduleCampaignRequest request, UUID actorUserId) {
        try {
            TenantContext.setCompanyId(companyId);
            Campaign campaign = requireOwned(companyId, campaignId);
            requireChannelAttached(campaignId);
            requireAudienceNotEmpty(campaign);
            if (request.scheduledAt() == null || request.scheduledAt().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Data de agendamento deve estar no futuro.");
            }

            if (campaign.getStatus() == CampaignStatus.DRAFT) {
                campaign.transitionTo(CampaignStatus.SCHEDULED);
            } else if (campaign.getStatus() != CampaignStatus.SCHEDULED) {
                throw new IllegalStateException(
                        "Somente campanhas DRAFT ou SCHEDULED podem ser agendadas.");
            }
            campaign.setScheduledAt(request.scheduledAt());
            if (request.timezone() != null && !request.timezone().isBlank()) {
                // timezone é imutável após criação nesta sprint; valor novo é ignorado
                // e o agendamento usa o timezone da campanha.
            }
            Campaign saved = campaignRepository.save(campaign);

            CampaignAudit.record(auditor, companyId, AuditAction.UPDATE, saved.getId(),
                    "Campanha agendada para " + request.scheduledAt(), actorUserId,
                    Map.of("scheduledAt", request.scheduledAt().toString()));
            return withChannel(saved);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public com.becommerce.crm.application.campaign.dto.ExecutionResponse executeNow(
            UUID companyId, UUID campaignId, UUID actorUserId) {
        try {
            TenantContext.setCompanyId(companyId);
            Campaign campaign = requireOwned(companyId, campaignId);
            if (campaign.getStatus() == CampaignStatus.DRAFT) {
                requireChannelAttached(campaignId);
                requireAudienceNotEmpty(campaign);
                campaign.transitionTo(CampaignStatus.SCHEDULED);
                campaign.setScheduledAt(LocalDateTime.now());
                campaignRepository.save(campaign);
            }
            var response = executionService.startExecution(companyId, campaignId, actorUserId);

            CampaignAudit.record(auditor, companyId, AuditAction.CUSTOM, campaignId,
                    "Execução iniciada manualmente", actorUserId, Map.of());
            return response;
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public CampaignResponse pause(UUID companyId, UUID campaignId, UUID actorUserId) {
        try {
            TenantContext.setCompanyId(companyId);
            executionService.pause(companyId, campaignId);
            Campaign campaign = requireOwned(companyId, campaignId);

            CampaignAudit.record(auditor, companyId, AuditAction.CUSTOM, campaignId,
                    "Campanha pausada", actorUserId, Map.of());
            return withChannel(campaign);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public CampaignResponse resume(UUID companyId, UUID campaignId, UUID actorUserId) {
        try {
            TenantContext.setCompanyId(companyId);
            executionService.resume(companyId, campaignId);
            Campaign campaign = requireOwned(companyId, campaignId);

            CampaignAudit.record(auditor, companyId, AuditAction.CUSTOM, campaignId,
                    "Campanha retomada", actorUserId, Map.of());
            return withChannel(campaign);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public CampaignResponse cancel(UUID companyId, UUID campaignId, UUID actorUserId) {
        try {
            TenantContext.setCompanyId(companyId);
            Campaign campaign = requireOwned(companyId, campaignId);
            if (campaign.getStatus() == CampaignStatus.COMPLETED
                    || campaign.getStatus() == CampaignStatus.CANCELLED) {
                throw new IllegalStateException("Campanha já finalizada (" + campaign.getStatus() + ").");
            }
            executionService.cancel(companyId, campaignId);
            campaign.transitionTo(CampaignStatus.CANCELLED);
            Campaign saved = campaignRepository.save(campaign);

            CampaignAudit.record(auditor, companyId, AuditAction.CUSTOM, campaignId,
                    "Campanha cancelada", actorUserId, Map.of("previousStatus", campaign.getStatus().name()));
            return withChannel(saved);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public com.becommerce.crm.application.campaign.dto.ExecutionResponse getExecution(
            UUID companyId, UUID campaignId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwned(companyId, campaignId);
            return executionService.getExecution(companyId, campaignId);
        } finally {
            TenantContext.clear();
        }
    }

    private void requireChannelAttached(UUID campaignId) {
        if (eventRepository.findChannelByCampaignId(campaignId).isEmpty()) {
            throw new IllegalStateException("Campanha precisa de canal + template antes de ser agendada/executada.");
        }
    }

    private void requireAudienceNotEmpty(Campaign campaign) {
        List<AudienceResolver.Recipient> recipients = audienceResolver.resolve(
                campaign.getCompanyId(), campaign.getAudienceType(), campaign.getAudienceCriteria());
        if (recipients.isEmpty()) {
            throw new IllegalStateException("Público da campanha está vazio.");
        }
        campaign.setEstimatedRecipients(recipients.size());
    }

    private Campaign requireOwned(UUID companyId, UUID campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new CampaignNotFoundException(campaignId));
        if (!campaign.getCompanyId().equals(companyId)) {
            throw new CampaignNotFoundException(campaignId);
        }
        return campaign;
    }

    private String normalizeEnum(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return CampaignStatus.valueOf(raw).name();
        } catch (IllegalArgumentException ignored) {
            // tenta como AudienceType abaixo
        }
        try {
            return AudienceType.valueOf(raw).name();
        } catch (IllegalArgumentException ignored) {
            // devolve como veio
        }
        return raw;
    }

    private CampaignResponse withChannel(Campaign c) {
        var channel = eventRepository.findChannelByCampaignId(c.getId()).orElse(null);
        if (channel == null) {
            return toResponse(c);
        }
        return new CampaignResponse(c.getId(), c.getCompanyId(), c.getName(), c.getDescription(),
                c.getStatus(), c.getAudienceType(), c.getAudienceCriteria(),
                c.getEstimatedRecipients(), c.getScheduledAt(), c.getTimezone(),
                c.getStartedAt(), c.getCompletedAt(), c.getCreatedBy(),
                channel.getId(), channel.getChannelType(), channel.getProviderChannelId(),
                channel.getTemplateId(), channel.getTemplateVersion(),
                c.getCreatedAt(), c.getUpdatedAt());
    }

    private static CampaignResponse toResponse(Campaign c) {
        return new CampaignResponse(c.getId(), c.getCompanyId(), c.getName(), c.getDescription(),
                c.getStatus(), c.getAudienceType(), c.getAudienceCriteria(),
                c.getEstimatedRecipients(), c.getScheduledAt(), c.getTimezone(),
                c.getStartedAt(), c.getCompletedAt(), c.getCreatedBy(),
                null, null, null, null, null, c.getCreatedAt(), c.getUpdatedAt());
    }
}
