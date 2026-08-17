package com.becommerce.crm.application.ai.service;

import com.becommerce.crm.application.ai.dto.AiSuggestionResponse;
import com.becommerce.crm.application.ai.port.input.AiSuggestionUseCase;
import com.becommerce.crm.application.ai.port.output.AiSuggestionProvider;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelMessageRepository;
import com.becommerce.crm.domain.omnichannel.Conversation;
import com.becommerce.crm.domain.omnichannel.Message;
import com.becommerce.crm.domain.omnichannel.MessageDirection;
import com.becommerce.crm.domain.omnichannel.OmnichannelNotFoundException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Sugestão de resposta com IA (Sprint 20). Orquestra a recuperação do histórico
 * da conversa (omnichannel) e a chamada ao {@link AiSuggestionProvider},
 * mantendo o isolamento por tenant (RLS) via {@link TenantContext}.
 */
@Service
public class AiSuggestionService implements AiSuggestionUseCase {

    private static final Logger log = LoggerFactory.getLogger(AiSuggestionService.class);

    /** Número máximo de mensagens recentes enviadas ao prompt. */
    private static final int HISTORY_LIMIT = 20;

    private final OmnichannelConversationRepository conversationRepository;
    private final OmnichannelMessageRepository messageRepository;
    private final AiSuggestionProvider aiSuggestionProvider;

    public AiSuggestionService(OmnichannelConversationRepository conversationRepository,
                               OmnichannelMessageRepository messageRepository,
                               AiSuggestionProvider aiSuggestionProvider) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.aiSuggestionProvider = aiSuggestionProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public AiSuggestionResponse suggest(UUID companyId, UUID conversationId) {
        try {
            TenantContext.setCompanyId(companyId);
            Conversation conversation = requireOwned(companyId, conversationId);

            List<AiSuggestionProvider.MessageLine> history = buildHistory(conversationId);

            String suggestion = aiSuggestionProvider.suggest(
                    new AiSuggestionProvider.SuggestRequest(companyId, conversationId, history));

            return new AiSuggestionResponse(conversationId, suggestion, aiSuggestionProvider.providerName());
        } finally {
            TenantContext.clear();
        }
    }

    private Conversation requireOwned(UUID companyId, UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new OmnichannelNotFoundException(conversationId, "Conversa"));
        if (!conversation.getCompanyId().equals(companyId)) {
            throw new OmnichannelNotFoundException(conversationId, "Conversa");
        }
        return conversation;
    }

    private List<AiSuggestionProvider.MessageLine> buildHistory(UUID conversationId) {
        // Mensagens da conversa (persistência ordena por createdAt ASC). Página
        // grande o suficiente para prover contexto inicial da conversa.
        var page = messageRepository.findByConversation(conversationId, 0, HISTORY_LIMIT * 2);
        return page.content().stream()
                .filter(m -> m.getDirection() == MessageDirection.INBOUND
                        || m.getDirection() == MessageDirection.OUTBOUND)
                .filter(m -> m.getBody() != null && !m.getBody().isBlank())
                .map(m -> new AiSuggestionProvider.MessageLine(
                        m.getDirection() == MessageDirection.INBOUND ? "customer" : "assistant",
                        m.getBody()))
                .toList();
    }
}