package com.becommerce.crm.infrastructure.integration.openai;

import com.becommerce.crm.application.ai.port.output.AiSuggestionProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Provider fake de IA para desenvolvimento/simulação (Sprint 20, Módulo de IA).
 * NÃO faz chamadas externas: gera uma sugestão determinística a partir do
 * histórico. Em produção, o adapter OpenAI (OpenAiSuggestionProvider)
 * implementa a mesma porta {@link AiSuggestionProvider}, selecionado por
 * {@code app.ai.provider=openai}.
 */
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "fake", matchIfMissing = true)
public class FakeAiSuggestionProvider implements AiSuggestionProvider {

    @Override
    public String suggest(SuggestRequest request) {
        String last = lastCustomerMessage(request.history());
        if (last == null || last.isBlank()) {
            return "Olá! Como posso ajudar você hoje?";
        }
        return "Obrigado pela sua mensagem: \"" + truncate(last) + "\". Estamos analisando e "
                + "retornaremos em breve. Há mais alguma coisa em que possamos ajudar?";
    }

    @Override
    public String providerName() {
        return "FAKE";
    }

    private String lastCustomerMessage(List<MessageLine> history) {
        if (history == null || history.isEmpty()) {
            return null;
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            if ("customer".equals(history.get(i).role())) {
                return history.get(i).content();
            }
        }
        return null;
    }

    private String truncate(String value) {
        return value.length() <= 60 ? value : value.substring(0, 60) + "...";
    }
}