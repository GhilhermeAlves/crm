package com.becommerce.crm.application.ai.action;

import com.becommerce.crm.application.ai.context.AiPermissionContext;
import com.becommerce.crm.application.ai.dto.AiActionResponse;
import com.becommerce.crm.application.ai.port.input.AiActionUseCase;
import com.becommerce.crm.application.ai.port.output.AiActionRepository;
import com.becommerce.crm.application.ai.port.output.AiChatRepository;
import com.becommerce.crm.application.ai.tool.AiTool;
import com.becommerce.crm.application.ai.tool.AiToolRegistry;
import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.domain.ai.AiAction;
import com.becommerce.crm.domain.ai.AiActionInvalidStateException;
import com.becommerce.crm.domain.ai.AiActionNotFoundException;
import com.becommerce.crm.domain.ai.AiActionStatus;
import com.becommerce.crm.domain.ai.AiConversation;
import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.audit.AuditModule;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service de acoes de escrita do assistente de IA (AI-05). A proposta e criada
 * durante o chat ({@link #propose}) sem efeito de escrita; a confirmacao
 * ({@link #confirm}) executa os parametros PERSISTIDOS sob lock pessimista,
 * garantindo: (a) posse por usuario, (b) permissao de negocio da ferramenta,
 * (c) transicao PROPOSED -> terminal atomica (confirmacoes concorrentes sao
 * idempotentes), (d) auditoria de cada transicao. {@link #cancel} recusa uma
 * proposta {@code PROPOSED}. {@link #listByConversation} expoe as acoes de uma
 * conversa do usuario para reconstrucao de historico.
 */
@Service
public class AiActionService implements AiActionUseCase {

    private static final Logger log = LoggerFactory.getLogger(AiActionService.class);

    private final AiActionRepository actionRepository;
    private final AiChatRepository chatRepository;
    private final AiToolRegistry toolRegistry;
    private final AiActionExecutor executor;
    private final TenantAuditRecorder auditor;

    public AiActionService(AiActionRepository actionRepository,
                           AiChatRepository chatRepository,
                           AiToolRegistry toolRegistry,
                           AiActionExecutor executor,
                           TenantAuditRecorder auditor) {
        this.actionRepository = actionRepository;
        this.chatRepository = chatRepository;
        this.toolRegistry = toolRegistry;
        this.executor = executor;
        this.auditor = auditor;
    }

    /**
     * Cria uma proposta {@code PROPOSED} (sem efeito de escrita). Chamada pelas
     * write tools durante o chat, sob o contexto confiavel derivado do
     * {@code CurrentUser}. Valida a posse da conversa antes de persistir.
     */
    @Transactional
    public AiActionResponse propose(UUID companyId, UUID userId, UUID conversationId, String tool,
                                    String entityType, UUID entityId, Map<String, Object> parameters,
                                    String description) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedConversation(companyId, userId, conversationId);
            AiAction action = AiAction.propose(companyId, userId, conversationId, tool, entityType,
                    entityId, parameters, description);
            actionRepository.save(action);
            audit(companyId, userId, action, "AI_ACTION_PROPOSED",
                    "Proposta de acao de escrita: " + description);
            return AiActionResponse.from(action);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public AiActionResponse confirm(UUID companyId, UUID userId, List<String> permissions, UUID actionId) {
        try {
            TenantContext.setCompanyId(companyId);
            AiAction action = actionRepository.findByIdForUpdate(actionId)
                    .orElseThrow(() -> new AiActionNotFoundException(
                            "Acao nao encontrada ou sem acesso."));
            requireOwner(companyId, userId, action);

            AiActionStatus status = action.getStatus();
            if (status == AiActionStatus.EXECUTED || status == AiActionStatus.FAILED) {
                return AiActionResponse.from(action);
            }
            if (status != AiActionStatus.PROPOSED) {
                throw new AiActionInvalidStateException(
                        status == AiActionStatus.CANCELLED
                                ? "Esta acao foi cancelada e nao pode mais ser executada."
                                : "Esta acao nao esta mais pendente de confirmacao.");
            }

            AiTool tool = toolRegistry.find(action.getTool())
                    .orElseThrow(() -> new AiActionInvalidStateException(
                            "Ferramenta desconhecida: " + action.getTool()));
            if (!new AiPermissionContext(permissions).has(tool.requiredPermission())) {
                throw new CrmAccessDeniedException(
                        "Voce nao tem permissao para executar esta acao ("
                                + tool.requiredPermission() + ").");
            }

            action.markExecuting();
            actionRepository.save(action);

            try {
                Object result = executor.execute(action);
                // Services de dominio limpam o TenantContext em finally;
                // restauramos antes das operacoes de persistencia/auditoria.
                TenantContext.setCompanyId(companyId);
                action.markExecuted(result);
                actionRepository.save(action);
                audit(companyId, userId, action, "AI_ACTION_EXECUTED",
                        "Acao executada: " + action.getDescription());
                return AiActionResponse.from(action);
            } catch (Exception e) {
                log.warn("Falha ao executar acao de IA {}: {}", actionId, e.getMessage());
                TenantContext.setCompanyId(companyId);
                action.markFailed("Falha ao executar a acao: " + e.getMessage());
                actionRepository.save(action);
                audit(companyId, userId, action, "AI_ACTION_FAILED",
                        "Falha ao executar acao: " + action.getDescription());
                return AiActionResponse.from(action);
            }
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public AiActionResponse cancel(UUID companyId, UUID userId, UUID actionId) {
        try {
            TenantContext.setCompanyId(companyId);
            AiAction action = actionRepository.findByIdForUpdate(actionId)
                    .orElseThrow(() -> new AiActionNotFoundException(
                            "Acao nao encontrada ou sem acesso."));
            requireOwner(companyId, userId, action);
            if (action.getStatus() != AiActionStatus.PROPOSED) {
                throw new AiActionInvalidStateException(
                        "Apenas acoes pendentes podem ser canceladas.");
            }
            action.cancel();
            actionRepository.save(action);
            audit(companyId, userId, action, "AI_ACTION_CANCELLED",
                    "Acao cancelada: " + action.getDescription());
            return AiActionResponse.from(action);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiActionResponse> listByConversation(UUID companyId, UUID userId, UUID conversationId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedConversation(companyId, userId, conversationId);
            return actionRepository.findByConversationId(conversationId).stream()
                    .filter(a -> a.getUserId().equals(userId))
                    .map(AiActionResponse::from)
                    .toList();
        } finally {
            TenantContext.clear();
        }
    }

    private void requireOwnedConversation(UUID companyId, UUID userId, UUID conversationId) {
        AiConversation conversation = chatRepository.findConversationById(conversationId)
                .orElseThrow(() -> new AiActionNotFoundException(
                        "Conversa nao encontrada ou sem acesso."));
        if (!conversation.getCompanyId().equals(companyId)
                || !conversation.getUserId().equals(userId)) {
            throw new AiActionNotFoundException("Conversa nao encontrada ou sem acesso.");
        }
    }

    private void requireOwner(UUID companyId, UUID userId, AiAction action) {
        if (!action.getCompanyId().equals(companyId) || !action.getUserId().equals(userId)) {
            throw new AiActionNotFoundException("Acao nao encontrada ou sem acesso.");
        }
    }

    private void audit(UUID companyId, UUID userId, AiAction action, String event, String description) {
        try {
            auditor.record(companyId, AuditAction.CUSTOM, AuditModule.AI, "AiAction",
                    action.getId().toString(), description, userId,
                    Map.of(
                            "event", event,
                            "conversationId", action.getConversationId().toString(),
                            "tool", action.getTool(),
                            "entityType", action.getEntityType() != null ? action.getEntityType() : "",
                            "entityId", action.getEntityId() != null ? action.getEntityId().toString() : "",
                            "status", action.getStatus() != null ? action.getStatus().name() : "",
                            "result", action.getResult() != null ? action.getResult().toString() : ""));
        } catch (Exception e) {
            log.warn("Falha ao registrar auditoria de acao de IA {}: {}",
                    action.getId(), e.getMessage());
        }
    }
}