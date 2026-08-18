package com.becommerce.crm.infrastructure.integration.openai;

import com.becommerce.crm.application.ai.port.output.AiProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Provider fake de IA para o assistente (AI-01), usado em desenvolvimento e nos
 * testes por padrão. NÃO faz chamadas externas: responde de forma determinística
 * informando que os dados foram lidos a partir do contexto fornecido. Em
 * produção, o adapter OpenAI (OpenAiChatProvider) implementa a mesma porta
 * {@link AiProvider}, selecionado por {@code app.ai.provider=openai}.
 */
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "fake", matchIfMissing = true)
public class FakeAiChatProvider implements AiProvider {

    @Override
    public String chat(ChatRequest request) {
        String userMessage = lastUserMessage(request.messages());
        boolean hasContext = request.messages().stream().anyMatch(m -> "system".equals(m.role()));
        if (userMessage == null || userMessage.isBlank()) {
            return "Olá! Como posso ajudar com os dados comerciais da sua empresa?";
        }
        return (hasContext
                ? "Analisei o contexto do registro em foco. "
                : "Considere o contexto da sua empresa ativa. ")
                + "Pergunta recebida: \"" + truncate(userMessage)
                + "\". (Resposta simulada pelo provider fake — em produção a "
                + "resposta é gerada pelo modelo a partir dos dados do CRM.)";
    }

    @Override
    public String providerName() {
        return "FAKE";
    }

    private String lastUserMessage(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).role())) {
                return messages.get(i).content();
            }
        }
        return null;
    }

    private String truncate(String value) {
        return value.length() <= 80 ? value : value.substring(0, 80) + "...";
    }
}
