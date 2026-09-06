package com.becommerce.crm.application.ai.service;

import com.becommerce.crm.application.ai.dto.AgentConfigRequest;
import com.becommerce.crm.application.ai.dto.AgentConfigResponse;
import com.becommerce.crm.application.ai.port.input.AgentConfigUseCase;
import com.becommerce.crm.application.ai.port.output.AgentConfigRepository;
import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.domain.ai.AgentConfig;
import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.audit.AuditModule;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
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
            }

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

    private static AgentConfigResponse toResponse(AgentConfig c) {
        return new AgentConfigResponse(c.getId(), c.isAiEnabled(), c.isAllowAutoReply(),
                c.getSystemPrompt(), c.getModel(), c.getTemperature(), c.getMaxTokens(),
                c.getCooldownMinutes(), c.getMaxChars(), c.getUpdatedAt());
    }

    private static AgentConfigResponse defaultResponse() {
        return new AgentConfigResponse(null, false, false, null, null, null, null,
                DEFAULT_COOLDOWN_MINUTES, DEFAULT_MAX_CHARS, null);
    }
}