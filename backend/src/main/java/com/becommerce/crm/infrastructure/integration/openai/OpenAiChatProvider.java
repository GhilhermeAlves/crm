package com.becommerce.crm.infrastructure.integration.openai;

import com.becommerce.crm.application.ai.port.output.AiProvider;
import com.becommerce.crm.domain.ai.AiProviderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Adapter de produção do OpenAI (Chat Completions) para o assistente de IA
 * (AI-01). Ativo quando {@code app.ai.provider=openai} (default).
 *
 * <p>A API key vem de config/cofre ({@code app.ai.api-key}); nunca é logada nem
 * persistida. Reutiliza o mesmo endpoint/config do provider de sugestão.</p>
 *
 * <p>Sprint 2 (IA autônoma): aceita {@code AiProvider.GenerationParams} por
 * request (model/temperature/maxTokens/timeout). Timeout CENTRALIZADO aqui
 * (config {@code app.ai.timeout}), nunca espalhado pelo processador; quando
 * ocorre, vira {@code AiProviderException} RECUPERÁVEL para o failover. Erros
 * HTTP 429/408/425/5xx e de rede/timeout são recuperáveis; demais 4xx (config/
 * request inválidos) são NÃO recuperáveis (não provocam fallback indevido).</p>
 */
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
public class OpenAiChatProvider implements AiProvider {

    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final Duration timeout;

    public OpenAiChatProvider(
            WebClient.Builder webClientBuilder,
            @Value("${app.ai.openai.base-url:https://api.openai.com}") String baseUrl,
            @Value("${app.ai.api-key:}") String apiKey,
            @Value("${app.ai.model:gpt-4o-mini}") String model,
            @Value("${app.ai.timeout:30s}") Duration timeout) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
        this.timeout = timeout;
    }

    @Override
    public ChatResult chatWithTools(ChatRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiProviderException("Chave de API do OpenAI não configurada (app.ai.api-key).");
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        for (ChatMessage line : request.messages()) {
            messages.add(toApiMessage(line));
        }

        GenerationParams params = request.params() != null ? request.params() : GenerationParams.DEFAULT;

        try {
            Map<String, Object> bodyMap = new java.util.HashMap<>();
            bodyMap.put("model", params.model() != null ? params.model() : model);
            bodyMap.put("messages", messages);
            bodyMap.put("max_tokens", params.maxTokens() != null ? params.maxTokens() : 600);
            bodyMap.put("temperature", params.temperature() != null ? params.temperature() : 0.5);
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
                    .timeout(resolveTimeout(request))
                    .block();

            return extractChatResult(body);
        } catch (AiProviderException e) {
            throw e;
        } catch (RuntimeException e) {
            throw toAiProviderException(e);
        }
    }

    /**
     * Converte a mensagem interna para o formato da API, preservando o
     * protocolo de Tool Calling: assistant com {@code tool_calls} e resposta de
     * Tool com {@code tool_call_id} (a OpenAI rejeita role "tool" sem vínculo).
     */
    private Map<String, Object> toApiMessage(ChatMessage line) {
        Map<String, Object> msg = new java.util.LinkedHashMap<>();
        msg.put("role", mapRole(line.role()));
        if (line.content() != null) {
            msg.put("content", line.content());
        }
        if (line.toolCalls() != null && !line.toolCalls().isEmpty()) {
            msg.put("tool_calls", line.toolCalls().stream()
                    .map(tc -> Map.of("id", tc.id() != null ? tc.id() : "call_unknown",
                            "type", "function",
                            "function", Map.of("name", tc.name(), "arguments", toJson(tc.arguments()))))
                    .toList());
        }
        if (line.toolCallId() != null) {
            msg.put("tool_call_id", line.toolCallId());
        }
        return msg;
    }

    private String toJson(Map<String, Object> arguments) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
                    arguments != null ? arguments : Map.of());
        } catch (Exception e) {
            return "{}";
        }
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        String flat = value.replace("\n", " ").trim();
        return flat.length() > 300 ? flat.substring(0, 300) + "..." : flat;
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

        GenerationParams params = request.params() != null ? request.params() : GenerationParams.DEFAULT;

        List<Map<String, String>> messages = new ArrayList<>();
        for (ChatMessage line : request.messages()) {
            messages.add(Map.of("role", mapRole(line.role()), "content", line.content()));
        }

        try {
            Map<String, Object> bodyMap = new java.util.HashMap<>();
            bodyMap.put("model", params.model() != null ? params.model() : model);
            bodyMap.put("messages", messages);
            bodyMap.put("max_tokens", params.maxTokens() != null ? params.maxTokens() : 600);
            bodyMap.put("temperature", params.temperature() != null ? params.temperature() : 0.2);
            bodyMap.put("response_format", Map.of("type", "json_object"));

            Map<?, ?> body = webClient.post()
                    .uri("/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(bodyMap)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(resolveTimeout(request))
                    .block();

            String content = extractContent(body);
            if (content == null || content.isBlank()) {
                throw new AiProviderException("Resposta estruturada do OpenAI vazia.");
            }
            return content.trim();
        } catch (AiProviderException e) {
            throw e;
        } catch (RuntimeException e) {
            throw toAiProviderException(e);
        }
    }

    /**
     * Timeout centralizado: override do request (se o chamador tiver configurado)
     * senão o valor de infra {@code app.ai.timeout} deste provider.
     */
    private Duration resolveTimeout(ChatRequest request) {
        Duration override = request.params() != null ? request.params().timeout() : null;
        return override != null ? override : timeout;
    }

    /**
     * Classifica a exceção como RECUPERÁVEL (candidata a fallback) ou não.
     *
     * <p>Percorre a cadeia de causas: {@code block()} do WebClient re-embrulha
     * exceções checadas (ex.: {@link TimeoutException} do {@code Mono.timeout})
     * — assim o timeout continua sendo detectado e tratado como recuperável.</p>
     */
    static boolean isRecoverable(Throwable t) {
        boolean sawTransport = false;
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof WebClientResponseException w) {
                int status = w.getStatusCode().value();
                return status == 408 || status == 425 || status == 429 || status >= 500;
            }
            if (cur instanceof WebClientRequestException || cur instanceof TimeoutException
                    || cur instanceof java.net.SocketTimeoutException) {
                sawTransport = true;
            }
        }
        return sawTransport;
    }

    private static AiProviderException toAiProviderException(RuntimeException e) {
        String detail = e instanceof WebClientResponseException w
                ? w.getStatusCode() + " - " + truncate(w.getResponseBodyAsString())
                : e.getMessage();
        return new AiProviderException("Falha ao consultar o OpenAI: " + detail,
                e, isRecoverable(e));
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
