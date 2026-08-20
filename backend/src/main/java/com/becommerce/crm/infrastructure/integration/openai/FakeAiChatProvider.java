package com.becommerce.crm.infrastructure.integration.openai;

import com.becommerce.crm.application.ai.port.output.AiProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
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
            AiProvider.ToolDefinition first = request.tools().get(0);
            return ChatResult.withToolCalls(List.of(
                    new ToolCall("call_fake_1", first.name(), validArguments(first.inputSchema()))));
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

    /**
     * Análise contextual (AI-06): produz um JSON determinístico para testes a
     * partir do bloco de dados do CRM presente no prompt (delimitado por
     * {@code <crm_data>}). Se houver dados, gera resumo/inferência/recomendação;
     * caso contrário, um resumo neutro sem falsa certeza. Sempre JSON válido,
     * evitando parser frágil.
     */
    @Override
    public String chatStructured(ChatRequest request) {
        boolean hasCrmData = request.messages().stream()
                .anyMatch(m -> m.content() != null && m.content().contains("<crm_data>"));
        if (hasCrmData) {
            return "{\"summary\":\"Oportunidade em análise com base nos dados do CRM.\","
                    + "\"inferences\":[{\"key\":\"momentum\",\"text\":\"Pode haver perda de momentum comercial.\",\"confidence\":70}],"
                    + "\"recommendations\":[{\"key\":\"follow_up\",\"title\":\"Fazer follow-up\","
                    + "\"description\":\"Retomar o contato com o responsável pela oportunidade.\",\"priority\":80,"
                    + "\"justification\":\"A oportunidade está parada no estágio atual.\",\"action\":\"create_task\"}]}";
        }
        return "{\"summary\":\"Sem dados do CRM disponíveis para esta análise.\","
                + "\"inferences\":[],\"recommendations\":[]}";
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

    private Map<String, Object> validArguments(Map<String, Object> schema) {
        Map<String, Object> args = new LinkedHashMap<>();
        if (!(schema.get("required") instanceof List<?> required)) {
            return args;
        }
        if (!(schema.get("properties") instanceof Map<?, ?> properties)) {
            return args;
        }
        for (Object field : required) {
            Object property = properties.get(String.valueOf(field));
            if (property instanceof Map<?, ?> propertySchema) {
                args.put(String.valueOf(field), defaultValue(propertySchema));
            }
        }
        return args;
    }

    private Object defaultValue(Map<?, ?> propertySchema) {
        if (propertySchema.get("enum") instanceof List<?> enums && !enums.isEmpty()) {
            return enums.get(0);
        }
        if ("string".equals(propertySchema.get("type"))) {
            return "Registro do assistente";
        }
        if ("integer".equals(propertySchema.get("type")) || "number".equals(propertySchema.get("type"))) {
            return 1;
        }
        if ("boolean".equals(propertySchema.get("type"))) {
            return true;
        }
        if ("array".equals(propertySchema.get("type"))) {
            return List.of();
        }
        return "Registro do assistente";
    }
}
