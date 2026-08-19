package com.becommerce.crm.infrastructure.integration.openai;

import com.becommerce.crm.application.ai.port.output.AiProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Provider fake de IA para o assistente (AI-01/AI-03), usado em desenvolvimento
 * e nos testes por padrão. NÃO faz chamadas externas: responde de forma
 * determinística informando que os dados foram lidos a partir do contexto
 * fornecido. Em produção, o adapter OpenAI (OpenAiChatProvider) implementa a
 * mesma porta {@link AiProvider}, selecionado por {@code app.ai.provider=openai}.
 *
 * <p>Em AI-03, o fake simula o Tool Calling: quando o prompt inclui Tools e o
 * modelo ainda não recebeu resultado de Tool, ele solicita a primeira Tool
 * declarada (determinístico, para testes). Ao receber um resultado de Tool
 * (role {@code tool}), produz a resposta final a partir dele.</p>
 */
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "fake", matchIfMissing = true)
public class FakeAiChatProvider implements AiProvider {

    @Override
    public ChatResult chatWithTools(ChatRequest request) {
        boolean hasToolResult = request.messages().stream().anyMatch(m -> "tool".equals(m.role()));

        // 1) Já existe resultado de Tool: produz a resposta final.
        if (hasToolResult) {
            String lastTool = lastMessage(request.messages(), "tool");
            String userMessage = lastUserMessage(request.messages());
            return ChatResult.content("Analisei os dados do CRM (resultado da ferramenta: "
                    + truncate(String.valueOf(lastTool)) + "). Pergunta: \""
                    + truncate(userMessage == null ? "" : userMessage) + "\". "
                    + "(Resposta simulada pelo provider fake.)");
        }

        // 2) Sem resultado de Tool: se há Tools declaradas, solicita a primeira.
        if (request.tools() != null && !request.tools().isEmpty()) {
            String firstTool = request.tools().get(0).name();
            return ChatResult.withToolCalls(List.of(
                    new ToolCall("call_fake_1", firstTool, Map.of())));
        }

        // 3) Sem Tools: resposta textual simples.
        String userMessage = lastUserMessage(request.messages());
        if (userMessage == null || userMessage.isBlank()) {
            return ChatResult.content("Olá! Como posso ajudar com os dados comerciais da sua empresa?");
        }
        return ChatResult.content((hasContext(request)
                ? "Analisei o contexto do registro em foco. "
                : "Considere o contexto da sua empresa ativa. ")
                + "Pergunta recebida: \"" + truncate(userMessage)
                + "\". (Resposta simulada pelo provider fake — em produção a "
                + "resposta é gerada pelo modelo a partir dos dados do CRM.)");
    }

    @Override
    public String providerName() {
        return "FAKE";
    }

    private boolean hasContext(ChatRequest request) {
        return request.messages().stream().anyMatch(m -> "system".equals(m.role()));
    }

    private String lastUserMessage(List<ChatMessage> messages) {
        return lastMessage(messages, "user");
    }

    private String lastMessage(List<ChatMessage> messages, String role) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (role.equals(messages.get(i).role())) {
                return messages.get(i).content();
            }
        }
        return null;
    }

    private String truncate(String value) {
        return value.length() <= 80 ? value : value.substring(0, 80) + "...";
    }
}
