package com.becommerce.crm.infrastructure.integration.openai;

import com.becommerce.crm.application.ai.port.output.AiProvider;
import com.becommerce.crm.domain.ai.AiProviderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adapter de produção do OpenAI (Chat Completions) para o assistente de IA
 * (AI-01). Ativo SOMENTE quando {@code app.ai.provider=openai}; por padrão
 * usa-se o {@link FakeAiChatProvider}.
 *
 * <p>A API key vem de config/cofre ({@code app.ai.api-key}); nunca é logada nem
 * persistida. Reutiliza o mesmo endpoint/config do provider de sugestão.
 */
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
public class OpenAiChatProvider implements AiProvider {

    private final WebClient webClient;
    private final String apiKey;
    private final String model;

    public OpenAiChatProvider(
            WebClient.Builder webClientBuilder,
            @Value("${app.ai.openai.base-url:https://api.openai.com}") String baseUrl,
            @Value("${app.ai.api-key:}") String apiKey,
            @Value("${app.ai.model:gpt-4o-mini}") String model) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public ChatResult chatWithTools(ChatRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiProviderException("Chave de API do OpenAI não configurada (app.ai.api-key).");
        }

        List<Map<String, String>> messages = new ArrayList<>();
        for (ChatMessage line : request.messages()) {
            messages.add(Map.of("role", mapRole(line.role()), "content", line.content()));
        }

        try {
            Map<String, Object> bodyMap = new java.util.HashMap<>();
            bodyMap.put("model", model);
            bodyMap.put("messages", messages);
            bodyMap.put("max_tokens", 600);
            bodyMap.put("temperature", 0.5);
            if (request.tools() != null && !request.tools().isEmpty()) {
                bodyMap.put("tools", request.tools().stream()
                        .map(t -> Map.of("type", "function",
                                "function", Map.of("name", t.name(), "description", t.description(),
                                        "parameters", t.inputSchema())))
                        .toList());
            }

            Map<?, ?> body = webClient.post()
                    .uri("/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(bodyMap)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return extractChatResult(body);
        } catch (AiProviderException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new AiProviderException("Falha ao consultar o OpenAI: " + e.getMessage(), e);
        }
    }

    @Override
    public String providerName() {
        return "OPENAI";
    }

    /**
     * Análise contextual (AI-06): usa JSON mode do OpenAI para produzir o
     * contrato estruturado (resumo/inferências/recomendações). Melhor esforço —
     * a fidelidade final é validada pelo backend (parsing + fallback controlado).
     */
    @Override
    public String chatStructured(ChatRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiProviderException("Chave de API do OpenAI não configurada (app.ai.api-key).");
        }

        List<Map<String, String>> messages = new ArrayList<>();
        for (ChatMessage line : request.messages()) {
            messages.add(Map.of("role", mapRole(line.role()), "content", line.content()));
        }

        try {
            Map<String, Object> bodyMap = new java.util.HashMap<>();
            bodyMap.put("model", model);
            bodyMap.put("messages", messages);
            bodyMap.put("max_tokens", 600);
            bodyMap.put("temperature", 0.2);
            bodyMap.put("response_format", Map.of("type", "json_object"));

            Map<?, ?> body = webClient.post()
                    .uri("/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(bodyMap)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            String content = extractContent(body);
            if (content == null || content.isBlank()) {
                throw new AiProviderException("Resposta estruturada do OpenAI vazia.");
            }
            return content.trim();
        } catch (AiProviderException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new AiProviderException("Falha ao consultar o OpenAI: " + e.getMessage(), e);
        }
    }

    private String extractContent(Map<?, ?> body) {
        if (body == null) {
            return null;
        }
        Object choices = body.get("choices");
        if (choices instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> choice) {
            if (choice.get("message") instanceof Map<?, ?> msg) {
                Object content = msg.get("content");
                if (content != null) {
                    return content.toString();
                }
            }
        }
        return null;
    }

    private ChatResult extractChatResult(Map<?, ?> body) {
        if (body == null) {
            throw new AiProviderException("Resposta do OpenAI vazia.");
        }
        Object choices = body.get("choices");
        if (choices instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> choice) {
                Object message = choice.get("message");
                if (message instanceof Map<?, ?> msg) {
                    List<AiProvider.ToolCall> toolCalls = extractToolCalls(msg.get("tool_calls"));
                    if (!toolCalls.isEmpty()) {
                        return AiProvider.ChatResult.withToolCalls(toolCalls);
                    }
                    Object content = msg.get("content");
                    if (content != null && !content.toString().trim().isBlank()) {
                        return AiProvider.ChatResult.content(content.toString().trim());
                    }
                }
            }
        }
        throw new AiProviderException("Resposta do OpenAI sem conteúdo.");
    }

    private List<AiProvider.ToolCall> extractToolCalls(Object raw) {
        List<AiProvider.ToolCall> calls = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> tc) {
                    Object id = tc.get("id");
                    Object fn = tc.get("function");
                    if (fn instanceof Map<?, ?> f) {
                        Object name = f.get("name");
                        Object args = f.get("arguments");
                        Map<String, Object> parsedArgs = parseArguments(args);
                        calls.add(new AiProvider.ToolCall(
                                id != null ? id.toString() : null,
                                name != null ? name.toString() : "",
                                parsedArgs));
                    }
                }
            }
        }
        return calls;
    }

    private Map<String, Object> parseArguments(Object args) {
        if (args == null) {
            return Map.of();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(args.toString(), new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String mapRole(String role) {
        return "user".equals(role) || "assistant".equals(role) || "tool".equals(role) ? role : "user";
    }
}
