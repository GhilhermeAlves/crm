package com.becommerce.crm.application.followup.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.followup.dto.FollowUpRequest;
import com.becommerce.crm.application.followup.dto.FollowUpResponse;
import com.becommerce.crm.application.followup.port.input.FollowUpUseCase;
import com.becommerce.crm.application.followup.port.output.FollowUpRepository;
import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.audit.AuditModule;
import com.becommerce.crm.domain.followup.FollowUp;
import com.becommerce.crm.domain.followup.FollowUpAction;
import com.becommerce.crm.domain.followup.exception.FollowUpNotFoundException;
import com.becommerce.crm.domain.followup.exception.FollowUpValidationException;
import com.becommerce.crm.domain.omnichannel.Conversation;
import com.becommerce.crm.domain.omnichannel.OmnichannelNotFoundException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Casos de uso de FollowUp (Sprint 22). O tenant vem do usuário autenticado
 * (CurrentUser) — nunca do client — e é aplicado via {@link TenantContext}
 * + RLS FORCE. A auditoria reutiliza o {@link TenantAuditRecorder}.
 *
 * <p>Regras de negócio na criação: a conversa deve existir e pertencer à
 * empresa; follow-ups automáticos NÃO podem ser agendados enquanto a conversa
 * estiver em modo HUMAN (Human Takeover ativo) — o processador reaplicaria a
 * regra na execução de qualquer forma.</p>
 */
@Service
public class FollowUpService implements FollowUpUseCase {

    private static final Logger log = LoggerFactory.getLogger(FollowUpService.class);

    private final FollowUpRepository followUpRepository;
    private final OmnichannelConversationRepository conversationRepository;
    private final TenantAuditRecorder auditor;

    public FollowUpService(FollowUpRepository followUpRepository,
                           OmnichannelConversationRepository conversationRepository,
                           TenantAuditRecorder auditor) {
        this.followUpRepository = followUpRepository;
        this.conversationRepository = conversationRepository;
        this.auditor = auditor;
    }

    @Override
    @Transactional
    public FollowUpResponse create(UUID companyId, FollowUpRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            if (request.conversationId() == null || request.executeAt() == null || request.content() == null) {
                throw new FollowUpValidationException("conversationId, executeAt e content são obrigatórios");
            }
            Conversation conversation = requireOwnedConversation(companyId, request.conversationId());
            if (conversation.isInHumanMode()) {
                throw new FollowUpValidationException(
                        "Não é possível agendar follow-up automático em conversa com atendimento humano ativo");
            }

            if (request.idempotencyKey() != null) {
                FollowUp existing = followUpRepository.findByIdempotencyKey(companyId, request.idempotencyKey())
                        .orElse(null);
                if (existing != null) {
                    return FollowUpResponse.from(existing);
                }
            }

            FollowUp followUp = FollowUp.create(companyId, request.conversationId(), FollowUpAction.SEND_MESSAGE,
                    request.content(), request.executeAt(), request.idempotencyKey(), request.sequenceId());
            FollowUp saved = followUpRepository.save(followUp);
            audit(companyId, saved.getId(), AuditAction.CREATE,
                    "Follow-up agendado para " + request.executeAt());
            return FollowUpResponse.from(saved);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FollowUpResponse get(UUID companyId, UUID followUpId) {
        try {
            TenantContext.setCompanyId(companyId);
            return FollowUpResponse.from(requireOwnedFollowUp(companyId, followUpId));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FollowUpResponse> list(UUID companyId, UUID conversationId, int page, int pageSize) {
        try {
            TenantContext.setCompanyId(companyId);
            PageResponse<FollowUp> result = followUpRepository.findByCompany(companyId, conversationId, page, pageSize);
            return PageResponse.of(
                    result.content().stream().map(FollowUpResponse::from).toList(),
                    page, pageSize, result.totalElements());
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public FollowUpResponse cancel(UUID companyId, UUID followUpId) {
        try {
            TenantContext.setCompanyId(companyId);
            FollowUp followUp = requireOwnedFollowUp(companyId, followUpId);
            followUp.cancel();
            boolean cancelled = followUpRepository.cancelPending(companyId, followUpId, LocalDateTime.now());
            if (!cancelled) {
                throw new FollowUpValidationException("Follow-up não está mais pendente");
            }
            audit(companyId, followUpId, AuditAction.UPDATE, "Follow-up cancelado pelo atendente");
            return FollowUpResponse.from(followUp);
        } finally {
            TenantContext.clear();
        }
    }

    private FollowUp requireOwnedFollowUp(UUID companyId, UUID followUpId) {
        FollowUp followUp = followUpRepository.findById(followUpId)
                .orElseThrow(() -> new FollowUpNotFoundException(followUpId));
        if (!followUp.getCompanyId().equals(companyId)) {
            throw new FollowUpNotFoundException(followUpId);
        }
        return followUp;
    }

    private Conversation requireOwnedConversation(UUID companyId, UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new OmnichannelNotFoundException(conversationId, "Conversa"));
        if (!conversation.getCompanyId().equals(companyId)) {
            throw new OmnichannelNotFoundException(conversationId, "Conversa");
        }
        return conversation;
    }

    private void audit(UUID companyId, UUID followUpId, AuditAction action, String description) {
        try {
            auditor.record(companyId, action, AuditModule.FOLLOWUPS, "FollowUp",
                    followUpId.toString(), description, null, null);
        } catch (RuntimeException e) {
            log.warn("Falha ao registrar auditoria de FollowUp (company={}, followUp={}): {}",
                    companyId, followUpId, e.getMessage());
        }
    }
}