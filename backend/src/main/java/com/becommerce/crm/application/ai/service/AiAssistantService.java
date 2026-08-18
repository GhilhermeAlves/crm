package com.becommerce.crm.application.ai.service;

import com.becommerce.crm.application.ai.dto.AiChatRequest;
import com.becommerce.crm.application.ai.dto.AiChatResponse;
import com.becommerce.crm.application.ai.dto.AiContextPayload;
import com.becommerce.crm.application.ai.port.input.AiAssistantUseCase;
import com.becommerce.crm.application.ai.port.output.AiChatRepository;
import com.becommerce.crm.application.ai.port.output.AiProvider;
import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.domain.ai.AiConversation;
import com.becommerce.crm.domain.ai.AiConversationNotFoundException;
import com.becommerce.crm.domain.ai.AiMessage;
import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.audit.AuditModule;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orquestrador do assistente de IA (AI-01). Recebe a mensagem + dica de
 * contexto, resolve o contexto REAL de CRM (via {@link AiContextResolver}),
 * monta o prompt (sistema + contexto + histórico + pergunta), chama o
 * {@link AiProvider}, persiste a conversa/mensagens e registra auditoria.
 *
 * <p>Somente leitura nesta milestone — nenhuma ação de escrita é executada.
 * O isolamento por tenant é garantido pelo {@link TenantContext} (RLS) e por
 * checagens explícitas de posse da conversa.
 */
@Service
public class AiAssistantService implements AiAssistantUseCase {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);

    /** Número máximo de mensagens recentes enviadas como histórico ao modelo. */
    private static final int HISTORY_LIMIT = 20;

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";

    private final AiChatRepository chatRepository;
    private final AiProvider aiProvider;
    private final AiContextResolver contextResolver;
    private final TenantAuditRecorder auditor;

    public AiAssistantService(AiChatRepository chatRepository,
                              AiProvider aiProvider,
                              AiContextResolver contextResolver,
                              TenantAuditRecorder auditor) {
        this.chatRepository = chatRepository;
        this.aiProvider = aiProvider;
        this.contextResolver = contextResolver;
        this.auditor = auditor;
    }

    @Override
    @Transactional
    public AiChatResponse chat(UUID companyId, UUID userId, AiChatRequest request) {
        try {
            TenantContext.setCompanyId(companyId);

            AiConversation conversation = resolveConversation(companyId, userId, request);
            String resolvedContext = contextResolver.resolve(companyId, request.context());

            chatRepository.saveMessage(AiMessage.create(companyId, conversation.getId(), ROLE_USER, request.message()));

            List<AiProvider.ChatMessage> messages = buildPrompt(conversation, resolvedContext, request.message());

            String answer = aiProvider.chat(new AiProvider.ChatRequest(companyId, userId, messages));

            chatRepository.saveMessage(AiMessage.create(companyId, conversation.getId(), ROLE_ASSISTANT, answer));
            conversation.touch();
            chatRepository.saveConversation(conversation);

            audit(companyId, userId, conversation, request, resolvedContext);

            return new AiChatResponse(conversation.getId(), answer, aiProvider.providerName());
        } finally {
            TenantContext.clear();
        }
    }

    private AiConversation resolveConversation(UUID companyId, UUID userId, AiChatRequest request) {
        if (request.conversationId() == null) {
            AiContextPayload p = request.context();
            String screen = p != null ? p.screen() : null;
            UUID recordId = p != null ? p.recordId() : null;
            return chatRepository.saveConversation(
                    AiConversation.create(companyId, userId, screen, recordId, firstWords(request.message())));
        }
        AiConversation existing = chatRepository.findConversationById(request.conversationId())
                .orElseThrow(() -> new AiConversationNotFoundException(
                        "Conversa não encontrada ou sem acesso."));
        if (!existing.getCompanyId().equals(companyId) || !existing.getUserId().equals(userId)) {
            throw new AiConversationNotFoundException("Conversa não encontrada ou sem acesso.");
        }
        return existing;
    }

    private List<AiProvider.ChatMessage> buildPrompt(AiConversation conversation, String context,
                                                     String userMessage) {
        List<AiProvider.ChatMessage> messages = new ArrayList<>();
        messages.add(new AiProvider.ChatMessage("system", systemPrompt()));

        if (context != null && !context.isBlank()) {
            messages.add(new AiProvider.ChatMessage("system", context));
        }

        List<AiMessage> history = chatRepository.findMessagesByConversation(conversation.getId());
        int start = Math.max(0, history.size() - HISTORY_LIMIT);
        for (int i = start; i < history.size(); i++) {
            AiMessage m = history.get(i);
            if (!ROLE_USER.equals(m.getRole()) && !ROLE_ASSISTANT.equals(m.getRole())) {
                continue;
            }
            messages.add(new AiProvider.ChatMessage(m.getRole(), m.getContent()));
        }

        messages.add(new AiProvider.ChatMessage(ROLE_USER, userMessage));
        return messages;
    }

    private String systemPrompt() {
        return "Você é o assistente de inteligência comercial de um CRM (Customer 360). "
                + "Responda em português, de forma objetiva e profissional, com base APENAS nos "
                + "dados fornecidos no contexto. Se a informação não estiver disponível no contexto, "
                + "diga claramente que não tem os dados, sem inventar. Você tem acesso de LEITURA "
                + "aos dados da empresa ativa. Nunca mencione que recebeu um bloco de contexto técnico.";
    }

    private String firstWords(String message) {
        if (message == null) {
            return "Nova conversa";
        }
        String trimmed = message.trim();
        if (trimmed.length() <= 60) {
            return trimmed;
        }
        return trimmed.substring(0, 60) + "...";
    }

    private void audit(UUID companyId, UUID userId, AiConversation conversation, AiChatRequest request,
                       String context) {
        try {
            auditor.record(companyId, AuditAction.CUSTOM, AuditModule.AI, "AiConversation",
                    conversation.getId().toString(),
                    "Assistente de IA: " + request.message(),
                    userId,
                    Map.of("screen", conversation.getScreen() != null ? conversation.getScreen() : "",
                            "recordId", conversation.getRecordId() != null ? conversation.getRecordId().toString() : "",
                            "hasContext", context != null && !context.isBlank(),
                            "provider", aiProvider.providerName()));
        } catch (Exception e) {
            log.warn("Falha ao registrar auditoria de IA: {}", e.getMessage());
        }
    }
}
