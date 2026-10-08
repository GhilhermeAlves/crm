package com.becommerce.crm.automation.ai.application.service;

import com.becommerce.crm.automation.ai.application.dto.AgentConfigRequest;
import com.becommerce.crm.automation.ai.application.dto.AgentConfigResponse;
import com.becommerce.crm.automation.ai.application.port.input.AgentConfigUseCase;
import com.becommerce.crm.automation.ai.application.port.output.AgentConfigRepository;
import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.automation.ai.application.dto.AgentBehaviorDto;
import com.becommerce.crm.automation.ai.application.dto.AgentIdentityDto;
import com.becommerce.crm.automation.ai.domain.AgentBehavior;
import com.becommerce.crm.automation.ai.domain.AgentConfig;
import com.becommerce.crm.automation.ai.domain.AgentIdentity;
import com.becommerce.crm.automation.ai.domain.VoiceReplyMode;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Admin da configuração do agente de IA (Sprint 3-A). Reutiliza a infraestrutura
 * existente ({@link AgentConfigRepository} + V070/V071) sem duplicar camadas.
 *
 * <p>Safe defaults: a AUSÊNCIA de linha é um estado válido (agente desabilitado).
 * O GET retorna o default de fábrica com {@code id} nulo; o PUT cria a linha se
 * ela ainda não existir (upsert) — nunca lança erro por ausência.</p>
 *
 * <p>Tenant: {@link TenantContext} é definido a partir do {@code companyId} do
 * usuário autenticado (RLS FORCE). Auditoria reutiliza o {@link TenantAuditRecorder}
 * (Sprint 8.6).</p>
 */
@Service
public class AgentConfigAdminService implements AgentConfigUseCase {

    private static final Logger log = LoggerFactory.getLogger(AgentConfigAdminService.class);

    static final int DEFAULT_COOLDOWN_MINUTES = 60;
    static final int DEFAULT_MAX_CHARS = 1000;

    private final AgentConfigRepository agentConfigRepository;
    private final TenantAuditRecorder auditor;

    public AgentConfigAdminService(AgentConfigRepository agentConfigRepository,
                                   TenantAuditRecorder auditor) {
        this.agentConfigRepository = agentConfigRepository;
        this.auditor = auditor;
    }

    @Override
    @Transactional(readOnly = true)
    public AgentConfigResponse get(UUID companyId) {
        try {
            TenantContext.setCompanyId(companyId);
            return agentConfigRepository.findByCompanyId(companyId)
                    .map(AgentConfigAdminService::toResponse)
                    .orElseGet(AgentConfigAdminService::defaultResponse);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public AgentConfigResponse update(UUID companyId, AgentConfigRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            AgentConfig existing = agentConfigRepository.findByCompanyId(companyId).orElse(null);
            boolean created;
            AgentConfig updated;
            if (existing == null) {
                updated = AgentConfig.create(companyId, request.aiEnabled(), request.allowAutoReply(),
                        request.systemPrompt(), request.model(), request.temperature(),
                        request.maxTokens(), request.cooldownMinutes(), request.maxChars());
                created = true;
            } else {
                updated = existing.withSettings(request.aiEnabled(), request.allowAutoReply(),
                        request.systemPrompt(), request.model(), request.temperature(),
                        request.maxTokens(), request.cooldownMinutes(), request.maxChars());
                created = false;
                updated.withVoiceReplyMode(existing.getVoiceReplyMode());
            }
            updated.withVoiceReplyMode(VoiceReplyMode.parseOrDefault(request.voiceReplyMode(), null));
            updated.withProfile(toIdentity(request.identity()), toBehavior(request.behavior()),
                    request.memoryEnabled(), request.humanTransferEnabled());

            AgentConfig saved = agentConfigRepository.save(updated);
            audit(companyId, created);
            return toResponse(saved);
        } finally {
            TenantContext.clear();
        }
    }

    private void audit(UUID companyId, boolean created) {
        try {
            auditor.record(companyId, created ? AuditAction.CREATE : AuditAction.UPDATE,
                    AuditModule.AI, "AgentConfig", companyId.toString(),
                    created ? "Configuração do agente de IA criada"
                            : "Configuração do agente de IA atualizada",
                    null, null);
        } catch (RuntimeException e) {
            log.warn("Falha ao registrar auditoria do AgentConfig (company={}): {}", companyId, e.getMessage());
        }
    }

    private static AgentIdentity toIdentity(AgentIdentityDto dto) {
        return dto == null ? null : new AgentIdentity(dto.name(), dto.description(), dto.persona());
    }

    private static AgentBehavior toBehavior(AgentBehaviorDto dto) {
        return dto == null ? null : new AgentBehavior(dto.objective(), dto.tone(), dto.rules(), dto.instructions());
    }

    private static AgentConfigResponse toResponse(AgentConfig c) {
        String voice = c.getVoiceReplyMode().name();
        AgentIdentity identity = c.getIdentity();
        AgentBehavior behavior = c.getBehavior();
        return new AgentConfigResponse(c.getId(), c.isAiEnabled(), c.isAllowAutoReply(),
                c.getSystemPrompt(), c.getModel(), c.getTemperature(), c.getMaxTokens(),
                c.getCooldownMinutes(), c.getMaxChars(), c.getUpdatedAt(), voice,
                new AgentIdentityDto(identity.name(), identity.description(), identity.persona()),
                new AgentBehaviorDto(behavior.objective(), behavior.tone(), behavior.rules(), behavior.instructions()),
                new AgentConfigResponse.ModelSection(c.getModel(), c.getTemperature(), c.getMaxTokens()),
                new AgentConfigResponse.ConversationSection(c.getCooldownMinutes(), c.getMaxChars(), voice,
                        new AgentConfigResponse.MemorySection(c.isMemoryEnabled())),
                new AgentConfigResponse.ToolsSection(c.isHumanTransferEnabled()),
                !c.hasStructuredProfile());
    }

    private static AgentConfigResponse defaultResponse() {
        return new AgentConfigResponse(null, false, false, null, null, null, null,
                DEFAULT_COOLDOWN_MINUTES, DEFAULT_MAX_CHARS, null);
    }
}